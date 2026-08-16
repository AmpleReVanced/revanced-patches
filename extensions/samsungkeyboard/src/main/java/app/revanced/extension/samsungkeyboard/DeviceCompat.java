package app.revanced.extension.samsungkeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

public final class DeviceCompat {
    private static final String STORE_MODEL = "SM-S938B";
    private static final String STORE_ONE_UI_VERSION = "80500";

    private DeviceCompat() {
    }

    @SuppressLint({"HardwareIds", "MissingPermission"})
    public static String getSerial() {
        try {
            String serial = Build.getSerial();
            if (isUsable(serial)) return serial;
        } catch (SecurityException ignored) {
        }

        Context context = SettingsStore.getContext();
        if (context != null) {
            try {
                String androidId = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );
                if (isUsable(androidId)) return androidId;
            } catch (SecurityException ignored) {
            }
        }
        return Build.FINGERPRINT;
    }

    public static String getStoreModel(String model) {
        return isSamsungDevice() && isUsable(model) ? model : STORE_MODEL;
    }

    public static String getStoreOneUiVersion(String version) {
        if (isPositive(version)) return version;

        Context context = SettingsStore.getContext();
        if (context != null) {
            try {
                ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(
                        context.getPackageName(),
                        PackageManager.GET_META_DATA
                );
                int oneUiVersion = applicationInfo.metaData == null
                        ? 0
                        : applicationInfo.metaData.getInt("oneUiVersion");
                if (oneUiVersion > 0) return String.valueOf(oneUiVersion);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return STORE_ONE_UI_VERSION;
    }

    private static boolean isSamsungDevice() {
        return "samsung".equalsIgnoreCase(Build.MANUFACTURER);
    }

    private static boolean isPositive(String value) {
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isUsable(String value) {
        return value != null && !value.isEmpty() && !Build.UNKNOWN.equalsIgnoreCase(value);
    }
}