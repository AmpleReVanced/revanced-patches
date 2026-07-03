package app.revanced.extension.all.misc.signature.spoof;

import android.content.pm.PackageInfo;
import android.content.pm.VersionedPackage;
import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.Method;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

/**
 * Hooks {@code IPackageManager} so calls referring to the patched = renamed package return the
 * original signing certificate and original package name.
 *
 * <p>Intercepts four classes of IPC methods:
 * <ul>
 *   <li>{@code getInstallerPackageName} / {@code getInstallSourceInfo}: makes the Play Store look
 *       like the installer of the patched app (some integrity checks require this).</li>
 *   <li>{@code hasSigningCertificate}: returns {@code true} when the supplied certificate matches
 *       the original one, avoiding the signature mismatch branch used by some integrity checks
 *       that call into {@code PackageManager}.</li>
 *   <li>{@code getPackageInfo} / {@code getPackageInfoVersioned}: rewrites the requested package
 *       name to the currently-installed one and patches the returned {@link PackageInfo}.</li>
 * </ul>
 */
final class PackageManagerHook {

    private static final String TAG = SignatureSpoof.class.getName();
    private static final String PACKAGE_SERVICE = "package";
    private static final String IPACKAGEMANAGER_STUB = "android.content.pm.IPackageManager$Stub";
    private static final String IPACKAGEMANAGER = "android.content.pm.IPackageManager";

    private static volatile boolean installed = false;

    private PackageManagerHook() {
    }

    static synchronized void install() {
        if (installed) return;
        IInterface hookedInterface;
        try {
            hookedInterface = ServiceHook.replace(
                    PACKAGE_SERVICE,
                    IPACKAGEMANAGER_STUB,
                    IPACKAGEMANAGER,
                    PackageManagerHook::intercept);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to hook PackageManager", t);
            return;
        }
        if (hookedInterface != null) {
            try {
                ReflectionUtils.setStaticField(
                        "android.app.ActivityThread",
                        "sPackageManager",
                        hookedInterface);
                installed = true;
            } catch (Exception e) {
                Log.w(TAG, "Failed to replace sPackageManager", e);
            }
        }
    }

    private static Object intercept(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        if ("getInstallerPackageName".equals(name) && isTargetPackageArgument(args)) {
            return SignatureSpoof.getInstallerPackageName();
        }

        if ("getInstallSourceInfo".equals(name) && isTargetPackageArgument(args)) {
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/InstallSourceInfo;");
            Object[] rewritten = rewritePackageNameArg(args);
            Object result = ReflectionUtils.invoke(proxy, method, rewritten);
            SignatureSpoof.replaceInstallSourceInfo(result);
            return result;
        }

        if ("hasSigningCertificate".equals(name) && isTargetPackageArgument(args)
                && SignatureSpoof.isOriginalSigningCertificate(args)) {
            return Boolean.TRUE;
        }

        if (isPackageInfoMethod(name) && isTargetPackageArgument(args)) {
            args = rewritePackageInfoArgs(name, args);
        } else if (isPackageInfoMethod(name)) {
            args = rewritePackageInfoArgs(name, args);
        }

        Object result = ReflectionUtils.invoke(proxy, method, args);
        if (result instanceof PackageInfo && isPackageInfoMethod(name)) {
            SignatureSpoof.replacePackageInfo((PackageInfo) result);
        }
        return result;
    }

    private static boolean isPackageInfoMethod(String name) {
        return "getPackageInfo".equals(name) || "getPackageInfoVersioned".equals(name);
    }

    private static boolean isTargetPackageArgument(Object[] args) {
        if (args == null || args.length == 0) return false;
        if (!(args[0] instanceof String)) return false;
        return SignatureSpoof.isTargetPackage((String) args[0]);
    }

    /**
     * For {@code getPackageInfo}/{@code getPackageInfoVersioned} the requested package name may be
     * the original package (before patching renamed it), in which case the system call would fail.
     * Rewrites the first argument to the currently installed package name when the caller targets
     * the spoofed package, while preserving {@link VersionedPackage} wrappers.
     */
    private static Object[] rewritePackageInfoArgs(String name, Object[] args) {
        if (!isPackageInfoMethod(name) || args == null || args.length == 0 || args[0] == null) {
            return args;
        }
        String currentPackageName = SignatureSpoof.getCurrentPackageName();
        if (currentPackageName == null) return args;

        if (args[0] instanceof String && SignatureSpoof.isTargetPackage((String) args[0])) {
            Object[] rewritten = args.clone();
            rewritten[0] = currentPackageName;
            return rewritten;
        }
        if (args[0] instanceof VersionedPackage) {
            VersionedPackage original = (VersionedPackage) args[0];
            if (SignatureSpoof.isTargetPackage(original.getPackageName())) {
                Object[] rewritten = args.clone();
                rewritten[0] = new VersionedPackage(currentPackageName, original.getLongVersionCode());
                return rewritten;
            }
        }
        return args;
    }

    private static Object[] rewritePackageNameArg(Object[] args) {
        String currentPackageName = SignatureSpoof.getCurrentPackageName();
        if (currentPackageName == null || args == null || args.length == 0
                || !(args[0] instanceof String)) {
            return args;
        }
        Object[] rewritten = args.clone();
        rewritten[0] = currentPackageName;
        return rewritten;
    }
}