package com.yc.pm;

public class WebViewUtils {
    public static boolean isWebViewPackage(String pkg) {
        return pkg.contains("android")
                || pkg.contains("webview")
                || pkg.equals("com.android.chrome")
                || pkg.contains("vanadium");  // GrapheneOS
    }
}
