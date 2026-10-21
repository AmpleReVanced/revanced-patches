package app.revanced.extension.kakaotalk.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ListFilter {
    private ListFilter() {
    }

    public static <T> List<T> filter(List<T> items, Predicate<? super T> predicate) {
        if (items == null || items.isEmpty()) return items;

        ArrayList<T> filtered = null;
        for (int index = 0; index < items.size(); index++) {
            T item = items.get(index);
            if (predicate.test(item)) {
                if (filtered == null) filtered = new ArrayList<>(items.subList(0, index));
            } else if (filtered != null) {
                filtered.add(item);
            }
        }
        return filtered == null ? items : filtered;
    }
}