package app.revanced.extension.kakaotalk.patches;

import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class GhostModePatch {
    private GhostModePatch() {
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    public static Boolean noSeen(Boolean original) {
        return Settings.enableGhostReadOnSend() ? Boolean.TRUE : original;
    }

    public static Boolean notiRead(Boolean original) {
        return Settings.enableGhostRead() ? Boolean.FALSE : original;
    }

    public static long localReadLogId(long logId) {
        return Settings.enableGhostReadKeepUnread() ? Long.MIN_VALUE : logId;
    }

    public static long sentLogId(long logId) {
        return Settings.enableGhostReadKeepUnread() && Settings.enableGhostReadOnSend() ? Long.MIN_VALUE : logId;
    }

    public static boolean isChatRoomVisible(boolean visible) {
        return visible && !Settings.enableGhostReadKeepUnread();
    }

    public static boolean isUnreadByMe(long logId, long readLogId) {
        return Settings.enableGhostReadKeepUnread() && logId > readLogId;
    }
}
