package app.revanced.extension.dcinside.patches;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("unused")
public final class ShowLiveBestPatch {
    private ShowLiveBestPatch() {
    }

    public static List<?> replaceRecommendedItems(Object adapter, List<?> items, String spec) {
        int[] types = java.util.Arrays.stream(spec.split(",")).mapToInt(Integer::parseInt).toArray();
        int start = -1;
        int end = -1;
        boolean header = false;
        for (int i = 0; i < items.size(); i++) {
            int type = getType(items.get(i));
            if (type == types[0] || type == types[1]) {
                if (start < 0) start = i;
                header |= type == types[0];
                end = i + 1;
            } else if (start >= 0 && type == types[6]) {
                end = i + 1;
            } else if (start >= 0) {
                break;
            }
        }
        if (start < 0) return items;

        Enum<?> filter = getFilter(adapter);
        Enum<?> sort = getSort(adapter);
        List<?> posts = getPosts(adapter);
        ArrayList<Object> selected = new ArrayList<>();
        if (posts != null) {
            for (Object post : posts) {
                boolean lite = "light".equals(getCategory(post));
                if (!header || ("LITE".equals(filter.name()) ? lite : !"BEST".equals(filter.name()) || !lite)) {
                    selected.add(post);
                }
            }
        }
        boolean ranking = header && "RANKING".equals(filter.name());
        String metric = "RECOMMEND".equals(sort.name()) ? "recommend"
                : "REPLY".equals(sort.name()) ? "comment" : "hit";
        if (ranking) selected.sort(Comparator.comparingInt((Object post) -> getCount(post, metric)).reversed());
        ArrayList<Object> result = new ArrayList<>(items.subList(0, start));
        if (header) result.add(newItem(types[2], null, filter, sort, null, null));
        int limit = Math.min(selected.size(), header ? ranking ? 30 : selected.size() : 8);
        for (int i = 0; i < limit; i++) {
            Object post = selected.get(i);
            result.add(newItem(ranking ? types[5] : types[3], post, null, null,
                    i + 1, ranking ? getCount(post, metric) : null));
            result.add(newItem(types[6], null, null, null, null, null));
        }
        if (header) result.add(newItem(types[4], null, null, null, null, null));
        result.addAll(items.subList(end, items.size()));
        return result;
    }

    private static List<?> getPosts(Object adapter) {
        return null;
    }

    private static Enum<?> getFilter(Object adapter) {
        return null;
    }

    private static Enum<?> getSort(Object adapter) {
        return null;
    }

    private static int getType(Object item) {
        return 0;
    }

    private static String getCategory(Object post) {
        return null;
    }

    private static int getCount(Object post, String metric) {
        return 0;
    }

    private static Object newItem(int type, Object post, Object filter, Object sort, Integer rank, Integer count) {
        return null;
    }
}