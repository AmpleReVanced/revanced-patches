package app.revanced.extension.kakaotalk.patches;

import app.revanced.extension.kakaotalk.helper.ListFilter;

import java.util.List;

@SuppressWarnings("unused")
public final class HideMoreTabComponentsPatch {
    private HideMoreTabComponentsPatch() {
    }

    public static <T> List<T> filterItems(List<T> items) {
        return ListFilter.filter(items, HideMoreTabComponentsPatch::shouldHideItem);
    }

    private static boolean shouldHideItem(Object item) {
        return false;
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }
}
