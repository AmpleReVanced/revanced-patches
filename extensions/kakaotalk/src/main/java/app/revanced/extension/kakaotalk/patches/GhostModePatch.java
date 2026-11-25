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

    public static long viewedLogId(long logId) {
        return Settings.enableGhostRead() ? Long.MIN_VALUE : logId;
    }

    public static long sentLogId(long logId) {
        return Settings.enableGhostReadOnSend() ? Long.MIN_VALUE : logId;
    }

    public static boolean isUnreadByMe(long logId, long readLogId) {
        return Settings.enableGhostRead() && logId > readLogId;
    }
}
