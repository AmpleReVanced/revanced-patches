package app.revanced.extension.kakaotalk.patches;

import android.net.Uri;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import app.morphe.extension.shared.Logger;
import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class DisableSslPinningPatch {
    private static X509TrustManager proxyTrustManager;

    private DisableSslPinningPatch() {
    }

    public static boolean isEnabled() {
        return Settings.DISABLE_SSL_PINNING.get();
    }

    public static synchronized X509TrustManager getProxyTrustManager() {
        if (!isEnabled()) return null;
        if (proxyTrustManager != null) return proxyTrustManager;
        try {
            KeyStore store = KeyStore.getInstance("AndroidCAStore");
            store.load(null);
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(store);
            for (TrustManager manager : factory.getTrustManagers()) {
                if (manager instanceof X509TrustManager) {
                    proxyTrustManager = (X509TrustManager) manager;
                    return proxyTrustManager;
                }
            }
            throw new GeneralSecurityException("No X509 trust manager for AndroidCAStore");
        } catch (GeneralSecurityException | IOException exception) {
            Logger.printException(() -> "Could not load proxy CA certificates", exception);
            return null;
        }
    }

    public static SslErrorHandler filterWebViewSslError(SslErrorHandler handler, SslError error) {
        if (!isEnabled() || handler == null || error == null
                || error.getPrimaryError() != SslError.SSL_UNTRUSTED) return handler;
        for (int code = 0; code < SslError.SSL_MAX_ERROR; code++) {
            if (code != SslError.SSL_UNTRUSTED && error.hasError(code)) return handler;
        }
        Uri uri = Uri.parse(error.getUrl());
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                || error.getCertificate() == null) return handler;
        X509Certificate certificate = error.getCertificate().getX509Certificate();
        X509TrustManager manager = getProxyTrustManager();
        if (certificate == null || manager == null || !verifyHostname(host, certificate)) return handler;
        try {
            manager.checkServerTrusted(new X509Certificate[]{certificate}, certificate.getPublicKey().getAlgorithm());
            handler.proceed();
            return null;
        } catch (GeneralSecurityException exception) {
            Logger.printDebug(() -> "WebView proxy certificate rejected: " + exception);
            return handler;
        }
    }

    private static boolean verifyHostname(String host, X509Certificate certificate) {
        return false;
    }

    public static final class ProxyWebViewClient extends WebViewClient {
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (filterWebViewSslError(handler, error) != null) super.onReceivedSslError(view, handler, error);
        }
    }

    public static boolean isPatchIncluded() {
        return false;
    }
}
