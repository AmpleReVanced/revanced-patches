package app.revanced.extension.kakaotalk.patches;

import app.revanced.extension.kakaotalk.settings.Settings;
import kotlin.jvm.functions.Function2;

@SuppressWarnings("unused")
public final class DisableNavigationBarAnimationPatch {
    private DisableNavigationBarAnimationPatch() {
    }

    public static boolean shouldCompleteEntry(boolean navigationBar, boolean detailActive) {
        return navigationBar && !detailActive && Settings.disableNavigationBarAnimation();
    }

    public static boolean renderListPreview(boolean twoPane, boolean detailActive, boolean listSettledVisible,
                                            Function2<Object, Integer, ?> content, Object composer) {
        if (twoPane || (!detailActive && listSettledVisible) || !Settings.disableNavigationBarAnimation()) {
            return false;
        }
        content.invoke(composer, 0);
        return true;
    }

    public static boolean isPatchIncluded() {
        return false;
    }
}
