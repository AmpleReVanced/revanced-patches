package app.revanced.extension.kakaotalk.patches;

import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class DisableOpenChatRoomCommentPatch {
    private DisableOpenChatRoomCommentPatch() {
    }

    public static boolean commentsDisabled(boolean original) {
        return original || Settings.openChatRoomCommentDisabled();
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }
}
