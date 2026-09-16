package app.revanced.extension.kakaotalk.patches;

import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class RemoveShortFormTabPatch {
    private RemoveShortFormTabPatch() {
    }

    public static int getPageIndex(int position) {
        return Settings.removeShortFormTab() ? 0 : position;
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }
}
