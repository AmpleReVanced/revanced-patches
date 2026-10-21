package app.revanced.extension.kakaotalk.patches;

import app.revanced.extension.kakaotalk.helper.ListFilter;

import java.util.List;

public final class DisableOpenChatFeedAdPatch {
    private DisableOpenChatFeedAdPatch() {
    }

    public static <T> List<T> filterItems(List<T> items) {
        return ListFilter.filter(items, DisableOpenChatFeedAdPatch::shouldHideItem);
    }

    private static boolean shouldHideItem(Object item) {
        return false;
    }
}