package com.yc.pm;

import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;

import app.revanced.extension.kakaotalk.spoofer.Spoofer;

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
                     Object mSignatures = Reflect.on(mSigningDetails).get("mSignatures");
                     if (mSignatures != null && mSignatures.getClass().isArray()) {
                         Signature[] sigs = (Signature[]) mSignatures;
                         Spoofer.replaceSignature(sigs);
                     }
                }

                if (result.packageName != null && !result.packageName.equals("com.google.android.webview")) {
                    result.packageName = Spoofer.PACKAGE_NAME;
                }
            }
            return result;
        }
    }
}