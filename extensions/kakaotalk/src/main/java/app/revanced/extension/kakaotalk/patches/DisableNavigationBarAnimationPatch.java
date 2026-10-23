package app.revanced.extension.kakaotalk.patches;

import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class DisableNavigationBarAnimationPatch {
    private DisableNavigationBarAnimationPatch() {
    }

    public static boolean shouldAnimateBarReturn(boolean original) {
        return original && !Settings.disableNavigationBarAnimation();
    }

    public static boolean shouldCompleteEntry(boolean navigationBar, boolean detailActive) {
        return navigationBar && !detailActive && Settings.disableNavigationBarAnimation();
    }

    public static boolean isCompactDetailOpen(boolean original, boolean listPaneHidden, boolean backTransitionRunning) {
        return original && (!Settings.disableNavigationBarAnimation() || listPaneHidden || !backTransitionRunning);
    }

    public static boolean isPatchIncluded() {
        return false;
    }
}
