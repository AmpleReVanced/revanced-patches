package app.revanced.extension.all.misc.signature.spoof;

import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

/**
 * Loads the original APK signature / package name packaged by the patcher and exposes helpers used
 * by {@link PackageManagerHook} and {@link WebViewUpdateServiceHook} to spoof them at runtime.
 *
 * <p>The patcher writes the original signing certificate as a hex-encoded DER X.509 certificate in
 * one of {@link #SIGNATURE_RESOURCE}, {@link #SIGNATURE_RESOURCE_V31}, {@link #SIGNATURE_RESOURCE_V3}
 * or {@link #SIGNATURE_RESOURCE_V2}; the original package name in {@link #PACKAGE_NAME_RESOURCE};
 * and the original {@code AppComponentFactory} class name in
 * {@link SignatureSpoofAppComponentFactory#ORIGINAL_APP_COMPONENT_FACTORY_RESOURCE}.</p>
 */
final class SignatureSpoof {

    private static final String TAG = "SignatureSpoof";

    private static final String PLAY_STORE_PACKAGE_NAME = "com.android.vending";

    private static final String SIGNATURE_RESOURCE = "app.revanced.signature_spoof.sig";
    private static final String SIGNATURE_RESOURCE_V31 = "app.revanced.signature_spoof.sig.v31";
    private static final String SIGNATURE_RESOURCE_V3 = "app.revanced.signature_spoof.sig.v3";
    private static final String SIGNATURE_RESOURCE_V2 = "app.revanced.signature_spoof.sig.v2";

    private static final String PACKAGE_NAME_RESOURCE = "app.revanced.signature_spoof.package";

    private static Signature originalSignature;
    private static String originalPackageName;
    private static String currentPackageName;

    private SignatureSpoof() {
    }

    /**
     * Called from {@link SignatureSpoofAppComponentFactory} once a classloader and the current
     * package name are available. Loads bundled resources and installs the service hooks.
     */
    static void initialize(ClassLoader classLoader, String packageName) {
        if (packageName != null && !packageName.isEmpty()) {
            currentPackageName = packageName;
        }
        loadPackageName(classLoader);
        loadSignature(classLoader);
        installHooks();
    }

    static void installHooks() {
        try {
            HiddenApiBypass.addHiddenApiExemptions("");
        } catch (Throwable t) {
            Log.w(TAG, "Failed to exempt hidden APIs", t);
        }
        PackageManagerHook.install();
        WebViewUpdateServiceHook.install();
    }

    /**
     * Conditionally spoofs a {@link PackageInfo} returned by the package manager.
     */
    static void replacePackageInfo(PackageInfo packageInfo) {
        if (packageInfo == null) return;
        if (shouldSpoofPackage(packageInfo.packageName)) {
            replacePackageInfoUnchecked(packageInfo);
        }
    }

    /**
     * Unconditionally rewrites the signature / package name fields in a {@link PackageInfo}.
     * The caller is responsible for ensuring the package info belongs to the target package.
     */
    static void replacePackageInfoUnchecked(PackageInfo packageInfo) {
        if (packageInfo == null) return;
        replaceSignatureArray(packageInfo.signatures);
        if (Build.VERSION.SDK_INT >= 28 && packageInfo.signingInfo != null) {
            replaceSigningInfo(packageInfo.signingInfo);
        }
        if (originalPackageName != null) {
            packageInfo.packageName = originalPackageName;
        }
    }

    static boolean shouldSpoofPackage(String packageName) {
        return packageName != null && isTargetPackage(packageName);
    }

    static boolean isTargetPackage(String packageName) {
        if (packageName == null) return false;
        String current = getCurrentPackageName();
        return packageName.equals(current) || packageName.equals(originalPackageName);
    }

    static String getCurrentPackageName() {
        if (currentPackageName != null) return currentPackageName;
        try {
            Object result = ReflectionUtils.invokeStatic(
                    "android.app.ActivityThread",
                    "currentPackageName",
                    new Class[]{},
                    new Object[]{});
            if (result instanceof String && !((String) result).isEmpty()) {
                currentPackageName = (String) result;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get current package name", e);
        }
        return currentPackageName;
    }

    static void replaceInstallSourceInfo(Object installSourceInfo) {
        if (installSourceInfo == null) return;
        ReflectionUtils.setField(installSourceInfo, PLAY_STORE_PACKAGE_NAME, "mInitiatingPackageName");
        ReflectionUtils.setField(installSourceInfo, PLAY_STORE_PACKAGE_NAME, "mInstallingPackageName");
    }

    static String getInstallerPackageName() {
        return PLAY_STORE_PACKAGE_NAME;
    }

    /**
     * Implements {@code PackageManager.hasSigningCertificate}: the system passes the certificate
     * bytes in {@code args[1]} and a {@code type} hint in {@code args[2]} (0 = raw bytes,
     * 1 = SHA-256 digest).
     */
    static boolean isOriginalSigningCertificate(Object[] args) {
        if (originalSignature == null || args == null || args.length < 3) return false;
        if (!(args[1] instanceof byte[]) || !(args[2] instanceof Integer)) return false;

        byte[] candidate = (byte[]) args[1];
        int type = (Integer) args[2];
        byte[] originalBytes = originalSignature.toByteArray();

        if (type == 0) {
            return Arrays.equals(candidate, originalBytes);
        }
        if (type == 1) {
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                return Arrays.equals(candidate, sha256.digest(originalBytes));
            } catch (Exception e) {
                Log.w(TAG, "Failed to hash original signing certificate", e);
            }
        }
        return false;
    }

    private static void replaceSignatureArray(Signature[] signatures) {
        Signature replacement = originalSignature;
        if (replacement == null || signatures == null || signatures.length == 0) return;
        signatures[0] = replacement;
    }

    private static void replaceSigningInfo(SigningInfo signingInfo) {
        Object signingDetails = ReflectionUtils.getFieldOrNull(signingInfo, "mSigningDetails");
        if (signingDetails == null) return;

        // "mSignatures" is used on Android 13+ (API 33), "signatures" on older releases.
        replaceSignatureArrayField(signingDetails, "mSignatures", "signatures");
        replaceSignatureArrayField(signingDetails, "mPastSigningCertificates", "pastSigningCertificates");
    }

    private static void replaceSignatureArrayField(Object target, String... fieldNames) {
        Object value = ReflectionUtils.getFieldOrNull(target, fieldNames);
        if (value instanceof Signature[]) {
            replaceSignatureArray((Signature[]) value);
        }
    }

    private static void loadPackageName(ClassLoader classLoader) {
        String packageName = readFirstLine(classLoader, PACKAGE_NAME_RESOURCE);
        if (packageName != null && !packageName.isEmpty()) {
            originalPackageName = packageName;
        }
    }

    private static void loadSignature(ClassLoader classLoader) {
        String hex = readRuntimeSignatureHex(classLoader);
        if (hex == null || hex.isEmpty()) {
            Log.w(TAG, "Original signature resource is missing");
            return;
        }
        originalSignature = new Signature(hex);
    }

    /**
     * Returns the original signing certificate hex string, preferring the scheme-matching resource
     * for the running Android version so the loaded {@link Signature} exactly matches what the OS
     * would return natively for that scheme.
     */
    private static String readRuntimeSignatureHex(ClassLoader classLoader) {
        if (Build.VERSION.SDK_INT >= 33) {
            String v31 = readFirstLine(classLoader, SIGNATURE_RESOURCE_V31);
            if (v31 != null && !v31.isEmpty()) return v31;
        }
        String v3 = readFirstLine(classLoader, SIGNATURE_RESOURCE_V3);
        if (v3 != null && !v3.isEmpty()) return v3;
        String v2 = readFirstLine(classLoader, SIGNATURE_RESOURCE_V2);
        if (v2 != null && !v2.isEmpty()) return v2;
        return readFirstLine(classLoader, SIGNATURE_RESOURCE);
    }

    static String readFirstLine(ClassLoader classLoader, String resource) {
        try (InputStream input = classLoader.getResourceAsStream(resource)) {
            if (input == null) return null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                return line != null ? line.trim() : null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to read " + resource, e);
            return null;
        }
    }
}