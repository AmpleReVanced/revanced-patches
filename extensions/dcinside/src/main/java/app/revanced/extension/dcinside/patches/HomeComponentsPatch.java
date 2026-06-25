package app.revanced.extension.dcinside.patches;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import app.revanced.extension.dcinside.settings.Settings;

@SuppressWarnings("unused")
public final class HomeComponentsPatch {
    private static final int GROUP_SEARCH_MENU = 0;
    private static final int GROUP_RECENT_GALLERIES = 1;
    private static final int GROUP_RECOMMENDED_GALLERIES = 2;
    private static final int GROUP_GALLERY_RANKING = 3;
    private static final int GROUP_LIVE_BEST = 4;
    private static final int GROUP_RECOMMENDED_POSTS = 5;
    private static final int UNKNOWN_TYPE = Integer.MIN_VALUE;

    private static volatile TypeSpec cachedTypeSpec;
    private static volatile String cachedRawTypeSpec;
    private static volatile Class<?> cachedItemClass;
    private static volatile String cachedTypeGetterName;
    private static volatile Method cachedTypeGetter;

    private HomeComponentsPatch() {
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    public static List<?> filterMainItems(List<?> items, String typeGetterName, String rawTypeSpec) {
        if (items == null || items.isEmpty() || !Settings.hideAnyHomeComponent()) {
            return items;
        }

        TypeSpec typeSpec = getTypeSpec(rawTypeSpec);
        ArrayList<Object> filtered = null;

        for (int index = 0; index < items.size(); index++) {
            Object item = items.get(index);
            int type = getItemType(item, typeGetterName);

            if (type != UNKNOWN_TYPE && shouldHide(type, typeSpec)) {
                if (filtered == null) {
                    filtered = new ArrayList<>(items.size());
                    for (int copyIndex = 0; copyIndex < index; copyIndex++) {
                        filtered.add(items.get(copyIndex));
                    }
                }
                continue;
            }

            if (filtered != null) {
                filtered.add(item);
            }
        }

        return filtered == null ? items : filtered;
    }

    private static boolean shouldHide(int type, TypeSpec typeSpec) {
        return Settings.hideHomeSearchMenu() && typeSpec.contains(GROUP_SEARCH_MENU, type)
                || Settings.hideHomeRecentGalleries() && typeSpec.contains(GROUP_RECENT_GALLERIES, type)
                || Settings.hideHomeRecommendedGalleries() && typeSpec.contains(GROUP_RECOMMENDED_GALLERIES, type)
                || Settings.hideHomeGalleryRanking() && typeSpec.contains(GROUP_GALLERY_RANKING, type)
                || Settings.hideHomeLiveBest() && typeSpec.contains(GROUP_LIVE_BEST, type)
                || Settings.hideHomeRecommendedPosts() && typeSpec.contains(GROUP_RECOMMENDED_POSTS, type);
    }

    private static int getItemType(Object item, String typeGetterName) {
        if (item == null || typeGetterName == null) {
            return UNKNOWN_TYPE;
        }

        try {
            Method method = getTypeGetter(item.getClass(), typeGetterName);
            Object value = method.invoke(item);
            return value instanceof Number ? ((Number) value).intValue() : UNKNOWN_TYPE;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return UNKNOWN_TYPE;
        }
    }

    private static Method getTypeGetter(Class<?> itemClass, String typeGetterName) throws NoSuchMethodException {
        Method method = cachedTypeGetter;
        if (method != null && itemClass == cachedItemClass && typeGetterName.equals(cachedTypeGetterName)) {
            return method;
        }

        method = itemClass.getDeclaredMethod(typeGetterName);
        method.setAccessible(true);
        cachedItemClass = itemClass;
        cachedTypeGetterName = typeGetterName;
        cachedTypeGetter = method;
        return method;
    }

    private static TypeSpec getTypeSpec(String rawTypeSpec) {
        TypeSpec typeSpec = cachedTypeSpec;
        if (typeSpec != null && rawTypeSpec != null && rawTypeSpec.equals(cachedRawTypeSpec)) {
            return typeSpec;
        }

        typeSpec = TypeSpec.parse(rawTypeSpec);
        cachedRawTypeSpec = rawTypeSpec;
        cachedTypeSpec = typeSpec;
        return typeSpec;
    }

    private static final class TypeSpec {
        private final int[][] groups;

        private TypeSpec(int[][] groups) {
            this.groups = groups;
        }

        static TypeSpec parse(String rawTypeSpec) {
            String[] parts = rawTypeSpec == null ? new String[0] : rawTypeSpec.split(";", -1);
            int[][] groups = new int[6][];
            for (int index = 0; index < groups.length; index++) {
                groups[index] = index < parts.length ? parseGroup(parts[index]) : new int[0];
            }
            return new TypeSpec(groups);
        }

        boolean contains(int group, int type) {
            int[] types = groups[group];
            for (int value : types) {
                if (value == type) {
                    return true;
                }
            }
            return false;
        }

        private static int[] parseGroup(String rawGroup) {
            if (rawGroup == null || rawGroup.length() == 0) {
                return new int[0];
            }

            String[] parts = rawGroup.split(",");
            int[] values = new int[parts.length];
            int count = 0;
            for (String part : parts) {
                try {
                    values[count++] = Integer.parseInt(part);
                } catch (NumberFormatException ignored) {
                    // Ignore malformed values so a bad spec never breaks the home screen.
                }
            }

            if (count == values.length) {
                return values;
            }

            int[] compact = new int[count];
            System.arraycopy(values, 0, compact, 0, count);
            return compact;
        }
    }
}
