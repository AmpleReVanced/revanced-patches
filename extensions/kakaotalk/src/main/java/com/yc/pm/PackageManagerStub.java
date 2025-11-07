package com.yc.pm;

import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.util.Log;
import app.revanced.extension.kakaotalk.spoofer.Spoofer;
import kotlin.Suppress;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

/**
 * Created by yanchen on 18-1-28.
 */

public class PackageManagerStub extends MethodInvocationProxy<MethodInvocationStub<IInterface>> {
    private static String SERVICE = "package";

    public PackageManagerStub() {
        super(new MethodInvocationStub<>(getInterface()));
        init();

    }

    public static void replaceService() {
        PackageManagerStub serviceStub = new PackageManagerStub();
    }

    private static IInterface getInterface() {
        Object service = Reflect.on("android.os.ServiceManager").call("getService", SERVICE).get();

        IInterface asInterface = Reflect.on("android.content.pm.IPackageManager$Stub").call("asInterface", service).get();
        return asInterface;
    }

    private static IBinder getBinder() {
        return Reflect.on("android.os.ServiceManager").call("getService", SERVICE).get();
    }

    private void init() {
        addMethodProxy(new GetPackageInfo());
        addMethodProxy(new GetInstallSourceInfo());
        addMethodProxy(new GetInstallerPackageName());
        getBinder();

        try {
            BinderInvocationStub pmHookBinder = new BinderInvocationStub(getInvocationStub().getBaseInterface());
            pmHookBinder.copyMethodProxies(getInvocationStub());
            pmHookBinder.replaceService(SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        IInterface hookedPM = null;
        try {
            hookedPM = getInvocationStub().getProxyInterface();
            Object o = Reflect.on("android.app.ActivityThread").set("sPackageManager", hookedPM);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class GetPackageInfo extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getPackageInfo";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            PackageInfo result = (PackageInfo) method.invoke(who, args);
            if (result != null) {
                if (result.signatures != null) {
                    Spoofer.replaceSignature(result.signatures);
                }

                if (result.signingInfo != null) {
                     Object mSigningDetails = Reflect.on(result.signingInfo).get("mSigningDetails");
                     Object mSignatures;
                     if (isAndroid13OrAbove()) {
                         mSignatures = Reflect.on(mSigningDetails).get("mSignatures");
                     } else {
                         mSignatures = Reflect.on(mSigningDetails).get("signatures");
                     }
                     if (mSignatures != null && mSignatures.getClass().isArray()) {
                         Signature[] sigs = (Signature[]) mSignatures;
                         Spoofer.replaceSignature(sigs);
                     }
                }

                if (result.packageName != null && !result.packageName.contains("android")) {
                    result.packageName = Spoofer.PACKAGE_NAME;
                }
            }
            return result;
        }

        private boolean isAndroid13OrAbove() {
            return android.os.Build.VERSION.SDK_INT >= 33;
        }
    }

    @Suppress(names = "NewApi")
    private static class GetInstallSourceInfo extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getInstallSourceInfo";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/InstallSourceInfo;");

            String originalPackageName = (String) Spoofer.invokeStaticMethod(
                    "android.app.ActivityThread",
                    "currentPackageName",
                    new Class[]{},
                    new Object[]{}
            );

            Log.i("PATCHER", "originalPackageName: " + originalPackageName);

            if (args[0] != null && (args[0].equals(Spoofer.PACKAGE_NAME) || args[0].equals(originalPackageName))) {
                Object[] modifiedArgs = args.clone();
                modifiedArgs[0] = originalPackageName;

                InstallSourceInfo result = (InstallSourceInfo) method.invoke(who, modifiedArgs);

                if (result != null) {
                    Spoofer.setField(
                            "android.content.pm.InstallSourceInfo",
                            result,
                            "mInitiatingPackageName",
                            "com.android.vending"
                    );
                }

                return result;
            } else {
                return method.invoke(who, args);
            }
        }
    }

    private static class GetInstallerPackageName extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getInstallerPackageName";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String originalPackageName = (String) Spoofer.invokeStaticMethod(
                    "android.app.ActivityThread",
                    "currentPackageName",
                    new Class[]{},
                    new Object[]{}
            );

            if (args[0] != null && (args[0].equals(Spoofer.PACKAGE_NAME) || args[0].equals(originalPackageName))) {
                return "com.android.vending";
            } else {
                return method.invoke(who, args[0]);
            }
        }
    }
}