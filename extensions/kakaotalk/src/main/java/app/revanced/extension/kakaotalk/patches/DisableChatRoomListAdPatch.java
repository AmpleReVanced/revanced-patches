package app.revanced.extension.kakaotalk.patches;

import java.util.List;

import app.revanced.extension.kakaotalk.helper.ListFilter;

public final class DisableChatRoomListAdPatch {
    private DisableChatRoomListAdPatch() {
    }

    public static <T> List<T> filterItems(List<T> items) {
        return ListFilter.filter(items, DisableChatRoomListAdPatch::shouldHideItem);
    }

    private static boolean shouldHideItem(Object item) {
        return false;
    }
}
