package app.revanced.extension.samsungkeyboard;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.stream.Collectors;

public final class SettingsStore {
    private static final int SECURE = 0;
    private static final int SYSTEM = 1;
    private static final int GLOBAL = 2;
    private static final String FEEDBACK_SOUND = "sip_key_feedback_sound";
    private static final String FEEDBACK_VIBRATION = "sip_key_feedback_vibration";
    private static final String FEEDBACK_SOUND_VOLUME = "feedback_sound_volume";
    private static final String FEEDBACK_VIBRATION_STRENGTH = "feedback_vibration_strength";
    private static final String DISABLED_SYSTEM_INPUT_METHODS = "disabled_system_input_methods";
    @SuppressLint("StaticFieldLeak")
    private static volatile Context context;
    private static volatile SharedPreferences preferences;

    private SettingsStore() {
    }

    public static void initialize(Context context) {
        Context storageContext = context.createDeviceProtectedStorageContext();
        SettingsStore.context = storageContext;
        preferences = storageContext.getSharedPreferences("revanced_settings", Context.MODE_PRIVATE);
    }

    static Context getContext() {
        return context;
    }

    public static boolean isFeedbackSoundEnabled(ContentResolver resolver) {
        return systemGetInt(resolver, FEEDBACK_SOUND, 1) != 0;
    }

    public static void setFeedbackSoundEnabled(ContentResolver resolver, boolean enabled) {
        systemPutInt(resolver, FEEDBACK_SOUND, enabled ? 1 : 0);
    }

    public static boolean isFeedbackVibrationEnabled(ContentResolver resolver) {
        return systemGetInt(resolver, FEEDBACK_VIBRATION, 1) != 0;
    }

    public static void setFeedbackVibrationEnabled(ContentResolver resolver, boolean enabled) {
        systemPutInt(resolver, FEEDBACK_VIBRATION, enabled ? 1 : 0);
    }

    public static int getFeedbackSoundVolume() {
        return getPreferenceInt(FEEDBACK_SOUND_VOLUME, 100);
    }

    public static void setFeedbackSoundVolume(int volume) {
        putPreferenceInt(FEEDBACK_SOUND_VOLUME, clamp(volume));
    }

    public static int getFeedbackVibrationStrength() {
        return getPreferenceInt(FEEDBACK_VIBRATION_STRENGTH, 100);
    }

    public static void setFeedbackVibrationStrength(int strength) {
        putPreferenceInt(FEEDBACK_VIBRATION_STRENGTH, clamp(strength));
    }

    public static String secureGetString(ContentResolver resolver, String name) {
        return getString(SECURE, resolver, name);
    }

    public static int secureGetInt(ContentResolver resolver, String name) throws Settings.SettingNotFoundException {
        return getInt(SECURE, resolver, name, null);
    }

    public static int secureGetInt(ContentResolver resolver, String name, int defaultValue) {
        try {
            return getInt(SECURE, resolver, name, defaultValue);
        } catch (Settings.SettingNotFoundException ignored) {
            return defaultValue;
        }
    }

    public static float secureGetFloat(ContentResolver resolver, String name, float defaultValue) {
        return getFloat(SECURE, resolver, name, defaultValue);
    }

    public static boolean securePutString(ContentResolver resolver, String name, String value) {
        return putString(SECURE, resolver, name, value);
    }

    public static boolean securePutInt(ContentResolver resolver, String name, int value) {
        return putInt(SECURE, resolver, name, value);
    }

    public static String systemGetString(ContentResolver resolver, String name) {
        return getString(SYSTEM, resolver, name);
    }

    public static int systemGetInt(ContentResolver resolver, String name) throws Settings.SettingNotFoundException {
        return getInt(SYSTEM, resolver, name, null);
    }

    public static int systemGetInt(ContentResolver resolver, String name, int defaultValue) {
        try {
            return getInt(SYSTEM, resolver, name, defaultValue);
        } catch (Settings.SettingNotFoundException ignored) {
            return defaultValue;
        }
    }

    public static float systemGetFloat(ContentResolver resolver, String name, float defaultValue) {
        return getFloat(SYSTEM, resolver, name, defaultValue);
    }

    public static boolean systemPutString(ContentResolver resolver, String name, String value) {
        return putString(SYSTEM, resolver, name, value);
    }

    public static boolean systemPutInt(ContentResolver resolver, String name, int value) {
        return putInt(SYSTEM, resolver, name, value);
    }

    public static String globalGetString(ContentResolver resolver, String name) {
        return getString(GLOBAL, resolver, name);
    }

    public static int globalGetInt(ContentResolver resolver, String name) throws Settings.SettingNotFoundException {
        return getInt(GLOBAL, resolver, name, null);
    }

    public static int globalGetInt(ContentResolver resolver, String name, int defaultValue) {
        try {
            return getInt(GLOBAL, resolver, name, defaultValue);
        } catch (Settings.SettingNotFoundException ignored) {
            return defaultValue;
        }
    }

    public static float globalGetFloat(ContentResolver resolver, String name, float defaultValue) {
        return getFloat(GLOBAL, resolver, name, defaultValue);
    }

    public static boolean globalPutString(ContentResolver resolver, String name, String value) {
        return putString(GLOBAL, resolver, name, value);
    }

    public static boolean globalPutInt(ContentResolver resolver, String name, int value) {
        return putInt(GLOBAL, resolver, name, value);
    }

    private static String getString(int scope, ContentResolver resolver, String name) {
        if (hasLocalValue(scope, name)) return localValue(scope, name);

        try {
            String value = switch (scope) {
                case SECURE -> Settings.Secure.getString(resolver, name);
                case SYSTEM -> Settings.System.getString(resolver, name);
                default -> Settings.Global.getString(resolver, name);
            };
            return value != null || scope != SECURE ? value : secureStringFallback(name);
        } catch (SecurityException ignored) {
            return scope == SECURE ? secureStringFallback(name) : null;
        }
    }

    private static int getInt(
            int scope,
            ContentResolver resolver,
            String name,
            Integer defaultValue
    ) throws Settings.SettingNotFoundException {
        if (hasLocalValue(scope, name)) {
            try {
                return Integer.parseInt(localValue(scope, name));
            } catch (NumberFormatException | NullPointerException ignored) {
                if (defaultValue != null) return defaultValue;
                throw new Settings.SettingNotFoundException(name);
            }
        }

        try {
            if (defaultValue != null) {
                return switch (scope) {
                    case SECURE -> Settings.Secure.getInt(resolver, name, defaultValue);
                    case SYSTEM -> Settings.System.getInt(resolver, name, defaultValue);
                    default -> Settings.Global.getInt(resolver, name, defaultValue);
                };
            }
            return switch (scope) {
                case SECURE -> Settings.Secure.getInt(resolver, name);
                case SYSTEM -> Settings.System.getInt(resolver, name);
                default -> Settings.Global.getInt(resolver, name);
            };
        } catch (SecurityException ignored) {
            if (defaultValue != null) return defaultValue;
            throw new Settings.SettingNotFoundException(name);
        }
    }

    private static float getFloat(int scope, ContentResolver resolver, String name, float defaultValue) {
        if (hasLocalValue(scope, name)) {
            try {
                return Float.parseFloat(localValue(scope, name));
            } catch (NumberFormatException | NullPointerException ignored) {
                return defaultValue;
            }
        }

        try {
            return switch (scope) {
                case SECURE -> Settings.Secure.getFloat(resolver, name, defaultValue);
                case SYSTEM -> Settings.System.getFloat(resolver, name, defaultValue);
                default -> Settings.Global.getFloat(resolver, name, defaultValue);
            };
        } catch (SecurityException ignored) {
            return defaultValue;
        }
    }

    private static boolean putString(int scope, ContentResolver resolver, String name, String value) {
        try {
            boolean written = switch (scope) {
                case SECURE -> Settings.Secure.putString(resolver, name, value);
                case SYSTEM -> Settings.System.putString(resolver, name, value);
                default -> Settings.Global.putString(resolver, name, value);
            };
            if (written) {
                clearLocalValue(scope, name);
                return true;
            }
        } catch (SecurityException ignored) {
        }
        if (isSystemManaged(scope, name)) {
            clearLocalValue(scope, name);
            return false;
        }
        return putLocalValue(scope, resolver, name, value);
    }

    private static boolean putInt(int scope, ContentResolver resolver, String name, int value) {
        try {
            boolean written = switch (scope) {
                case SECURE -> Settings.Secure.putInt(resolver, name, value);
                case SYSTEM -> Settings.System.putInt(resolver, name, value);
                default -> Settings.Global.putInt(resolver, name, value);
            };
            if (written) {
                clearLocalValue(scope, name);
                return true;
            }
        } catch (SecurityException ignored) {
        }
        if (isSystemManaged(scope, name)) {
            clearLocalValue(scope, name);
            return false;
        }
        return putLocalValue(scope, resolver, name, Integer.toString(value));
    }

    private static String secureStringFallback(String name) {
        Context context = SettingsStore.context;
        if (context == null) return null;

        InputMethodManager manager = context.getSystemService(InputMethodManager.class);
        if (manager == null) return null;
        if (Settings.Secure.ENABLED_INPUT_METHODS.equals(name)) {
            return manager.getEnabledInputMethodList().stream()
                    .map(InputMethodInfo::getId)
                    .collect(Collectors.joining(":"));
        }
        if (Build.VERSION.SDK_INT >= 34 && Settings.Secure.DEFAULT_INPUT_METHOD.equals(name)) {
            InputMethodInfo method = manager.getCurrentInputMethodInfo();
            return method == null ? null : method.getId();
        }
        return null;
    }

    private static boolean hasLocalValue(int scope, String name) {
        SharedPreferences preferences = SettingsStore.preferences;
        boolean present = preferences != null && preferences.getBoolean(markerKey(scope, name), false);
        if (!present || !isSystemManaged(scope, name)) return present;
        clearLocalValue(scope, name);
        return false;
    }

    private static boolean isSystemManaged(int scope, String name) {
        return scope == SECURE && (
                Settings.Secure.DEFAULT_INPUT_METHOD.equals(name) ||
                        Settings.Secure.ENABLED_INPUT_METHODS.equals(name) ||
                        Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE.equals(name) ||
                        DISABLED_SYSTEM_INPUT_METHODS.equals(name)
        );
    }

    private static int getPreferenceInt(String name, int defaultValue) {
        SharedPreferences preferences = SettingsStore.preferences;
        if (preferences == null) return defaultValue;
        try {
            return clamp(preferences.getInt(name, defaultValue));
        } catch (ClassCastException ignored) {
            return defaultValue;
        }
    }

    private static void putPreferenceInt(String name, int value) {
        SharedPreferences preferences = SettingsStore.preferences;
        if (preferences != null) preferences.edit().putInt(name, value).apply();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String localValue(int scope, String name) {
        SharedPreferences preferences = SettingsStore.preferences;
        return preferences == null ? null : preferences.getString(valueKey(scope, name), null);
    }

    private static boolean putLocalValue(int scope, ContentResolver resolver, String name, String value) {
        SharedPreferences preferences = SettingsStore.preferences;
        if (preferences == null) return false;
        preferences.edit()
                .putBoolean(markerKey(scope, name), true)
                .putString(valueKey(scope, name), value)
                .apply();
        notifyChange(scope, resolver, name);
        return true;
    }

    private static void clearLocalValue(int scope, String name) {
        SharedPreferences preferences = SettingsStore.preferences;
        if (preferences == null) return;
        preferences.edit()
                .remove(markerKey(scope, name))
                .remove(valueKey(scope, name))
                .apply();
    }

    private static void notifyChange(int scope, ContentResolver resolver, String name) {
        Uri uri = switch (scope) {
            case SECURE -> Settings.Secure.getUriFor(name);
            case SYSTEM -> Settings.System.getUriFor(name);
            default -> Settings.Global.getUriFor(name);
        };
        try {
            resolver.notifyChange(uri, null);
        } catch (SecurityException ignored) {
        }
    }

    private static String markerKey(int scope, String name) {
        return scope + ":" + name + ":present";
    }

    private static String valueKey(int scope, String name) {
        return scope + ":" + name + ":value";
    }
}