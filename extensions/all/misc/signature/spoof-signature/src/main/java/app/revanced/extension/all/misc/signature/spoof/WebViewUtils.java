package app.revanced.extension.all.misc.signature.spoof;

public final class WebViewUtils {

    private WebViewUtils() {
    }

    /**
     * Heuristic detection of WebView / Chromium provider packages.
     *
     * <p>Signature spoofing must never target WebView packages, since the framework relies on the
     * real signatures of those packages to validate the active WebView implementation.</p>
     */
    public static boolean isWebViewPackage(String packageName) {
        return packageName.contains("android")
                || packageName.contains("webview")
                || packageName.equals("com.android.chrome")
                || packageName.contains("vanadium"); // GrapheneOS
    }
}