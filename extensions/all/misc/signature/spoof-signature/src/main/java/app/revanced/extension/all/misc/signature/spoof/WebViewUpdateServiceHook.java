package app.revanced.extension.all.misc.signature.spoof;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Hooks {@code IWebViewUpdateService.waitForAndGetProvider} so signature checks performed against
 * the WebView {@code PackageInfo} do not blow up when the patched app overrides WebView provider
 * resolution. Only the WebView provider itself is left untouched (see {@link WebViewUtils}).
 */
final class WebViewUpdateServiceHook {

    private static final String TAG = SignatureSpoof.class.getName();
    private static final String SERVICE = "webviewupdate";
    private static final String STUB = "android.webkit.IWebViewUpdateService$Stub";
    private static final String INTERFACE = "android.webkit.IWebViewUpdateService";

    private static volatile boolean installed = false;

    private WebViewUpdateServiceHook() {
    }

    static synchronized void install() {
        if (installed) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        try {
            ServiceHook.replace(SERVICE, STUB, INTERFACE, WebViewUpdateServiceHook::intercept);
            installed = true;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to hook WebViewUpdateService", t);
        }
    }

    private static Object intercept(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = ReflectionUtils.invoke(proxy, method, args);
        if (result != null && "waitForAndGetProvider".equals(method.getName())) {
            Object packageInfo = ReflectionUtils.getFieldOrNull(result, "packageInfo");
            if (packageInfo instanceof PackageInfo) {
                PackageInfo info = (PackageInfo) packageInfo;
                if (!WebViewUtils.isWebViewPackage(info.packageName)
                        && SignatureSpoof.shouldSpoofPackage(info.packageName)) {
                    SignatureSpoof.replacePackageInfoUnchecked(info);
                }
            }
        }
        return result;
    }
}