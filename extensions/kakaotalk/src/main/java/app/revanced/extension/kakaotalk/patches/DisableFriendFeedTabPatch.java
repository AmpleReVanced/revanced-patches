package app.revanced.extension.kakaotalk.patches;

import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class DisableFriendFeedTabPatch {
    private DisableFriendFeedTabPatch() {
    }

    public static boolean isFriendFeedEnabled(boolean original) {
        return original && !Settings.disableFriendFeedTab();
    }

    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }
}
