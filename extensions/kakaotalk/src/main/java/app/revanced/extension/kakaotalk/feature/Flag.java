package app.revanced.extension.kakaotalk.feature;

import java.util.HashMap;
import java.util.Map;

import app.revanced.extension.kakaotalk.settings.Settings;

public class Flag {
    private static final String OPEN_CHAT_ROOM_COMMENT_DISABLED = "OPEN_CHAT_ROOM_COMMENT_DISABLED";

    private static final Map<String, Boolean> flags = new HashMap<>();

    static {
        loadFlags();
    }

    private static void loadFlags() {
        String raw = getFeatureFlags();
        if (raw == null) {
            return;
        }

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

    public static String getFeatureFlags() {
        return null; // Modified during patching.
    }

    public static boolean canIntercept(String key) {
        return key != null && (isOpenChatRoomCommentDisabled(key) || flags.containsKey(key));
    }

    public static boolean intercept(String key) {
        if (key == null) {
            return false;
        }

        if (isOpenChatRoomCommentDisabled(key)) {
            return true;
        }

        Boolean value = flags.get(key);
        return value != null && value;
    }

    private static boolean isOpenChatRoomCommentDisabled(String key) {
        return Settings.openChatRoomCommentDisabled()
                && OPEN_CHAT_ROOM_COMMENT_DISABLED.equalsIgnoreCase(key);
    }
}
