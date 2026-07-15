package app.revanced.extension.kakaotalk.feature;

import java.util.HashMap;
import java.util.Map;

import app.revanced.extension.kakaotalk.settings.Settings;

/**
 * Reached from KakaoTalk's own feature flag getter, which runs before the extension has a context.
 * Reading settings that early throws, and letting that escape would take out every flag read for the
 * rest of the process, so nothing here may fail. Returning false leaves the app's own value in
 * place, which is the right answer while the overrides cannot be known yet.
 */
public class Flag {
    private static final String OPEN_CHAT_ROOM_COMMENT_DISABLED = "OPEN_CHAT_ROOM_COMMENT_DISABLED";

    private static final Map<String, Boolean> flags = new HashMap<>();
    private static String loadedFeatureFlags;

    private Flag() {
    }

    private static void reloadFlagsIfNeeded() {
        String raw = getEffectiveFeatureFlags();
        if (raw == null) {
            raw = "";
        }
        if (raw.equals(loadedFeatureFlags)) return;

        loadedFeatureFlags = raw;
        flags.clear();

        raw = raw.trim();
        if (raw.isEmpty()) {
            return;
        }

        for (String entry : raw.split(";")) {
            if (entry == null) {
                continue;
            }

            entry = entry.trim();
            if (entry.isEmpty()) {
                continue;
            }

            String[] parts = entry.split("=", 2);
            if (parts.length != 2) {
                continue;
            }

            String key = parts[0].trim();
            String value = parts[1].trim();

            if (key.isEmpty()) {
                continue;
            }

            if ("true".equalsIgnoreCase(value)) {
                flags.put(key, true);
            } else if ("false".equalsIgnoreCase(value)) {
                flags.put(key, false);
            }
        }
    }

    private static String getEffectiveFeatureFlags() {
        String settingValue = Settings.featureFlagOverrides();
        if (settingValue != null && !settingValue.trim().isEmpty()) {
            return settingValue;
        }

        return getFeatureFlags();
    }

    public static String getFeatureFlags() {
        return null; // Modified during patching.
    }

    public static boolean canIntercept(String key) {
        if (key == null) {
            return false;
        }

        try {
            reloadFlagsIfNeeded();
            return isOpenChatRoomCommentDisabled(key) || flags.containsKey(key);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean intercept(String key) {
        if (key == null) {
            return false;
        }

        try {
            reloadFlagsIfNeeded();
            if (isOpenChatRoomCommentDisabled(key)) {
                return true;
            }

            Boolean value = flags.get(key);
            return value != null && value;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isOpenChatRoomCommentDisabled(String key) {
        return Settings.openChatRoomCommentDisabled()
                && OPEN_CHAT_ROOM_COMMENT_DISABLED.equalsIgnoreCase(key);
    }
}
