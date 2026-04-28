package app.revanced.extension.kakaotalk.spoofer;

import android.content.pm.Signature;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Spoofer {
    public static String SIGNATURE_HEX = "";

    public static String PACKAGE_NAME = "com.kakao.talk";

    private static String currentPackageName;

    public static void setCurrentPackageName(String packageName) {
        if (packageName != null && !packageName.isEmpty()) {
            currentPackageName = packageName;
        }
    }

    public static String getCurrentPackageName() {
        if (currentPackageName != null) {
            return currentPackageName;
        }

        try {
            String packageName = (String) invokeStaticMethod(
                    "android.app.ActivityThread",
                    "currentPackageName",
                    new Class[]{},
                    new Object[]{}
            );
            setCurrentPackageName(packageName);
        } catch (Exception e) {
            Log.w("PATCHER", "Failed to get current package name", e);
        }

        return currentPackageName;
    }

    public static boolean shouldSpoofPackage(String packageName) {
        if (packageName == null) {
            return false;
        }

        if (PACKAGE_NAME.equals(packageName)) {
            return true;
        }

        String currentPackageName = getCurrentPackageName();
        return currentPackageName != null && currentPackageName.equals(packageName);
    }

    public static void setStaticField(String clz, String fieldName, Object value) throws Exception {
        Field f = Class.forName(clz).getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, value);
    }


    public static void setField(String clz, Object obj, String fieldName, Object value) throws Exception {
        Field f = Class.forName(clz).getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(obj, value);
    }

    public static <T> T getField(String clz, Object obj, String fieldName) throws Exception {
        Field f = Class.forName(clz).getDeclaredField(fieldName);
        f.setAccessible(true);
        return (T) f.get(obj);
    }

    public static Object invokeMethod(String clz, Object obj, String methodName, Class[] parameterTypes, Object[] parameters) throws Exception {
        Method method = Class.forName(clz).getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(obj, parameters);
    }

    public static Object invokeStaticMethod(String clz, String methodName, Class[] parameterTypes, Object[] parameters) throws Exception {
        Method method = Class.forName(clz).getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, parameters);
    }

    public static <T> T loadParcelBase64(String b64, Parcelable.Creator<T> creator) {
        Parcel p = Parcel.obtain();
        byte[] b = Base64.decode(b64, Base64.NO_WRAP);
        p.unmarshall(b, 0, b.length);
        p.setDataPosition(0);
        return creator.createFromParcel(p);
    }

    public static void replaceSignature(Signature[] signatures) {
        if (signatures == null || signatures.length == 0) {
            return;
        }

        if (SIGNATURE_HEX.isEmpty())
            Log.w("PATCHER", "SIG 로드 전 replaceSignature 호출");
        signatures[0] = new Signature(SIGNATURE_HEX);
    }
}