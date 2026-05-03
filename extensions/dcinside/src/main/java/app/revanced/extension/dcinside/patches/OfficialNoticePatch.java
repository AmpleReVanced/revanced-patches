package app.revanced.extension.dcinside.patches;

import java.util.Iterator;
import java.util.List;

import app.revanced.extension.dcinside.settings.Settings;

@SuppressWarnings("unused")
public final class OfficialNoticePatch {
    private static final String NOTICE_TAB_NAME = "NOTICE";
    private static final String OFFICIAL_AUTHOR_NAME = "운영자";

    private OfficialNoticePatch() {
    }

    public static void filterNoticeList(List<?> posts, Object tabType) {
        if (!hideOfficialNotices()) {
            return;
        }
        if (!isNoticeTab(tabType)) {
            return;
        }

        filterOfficialNotices(posts);
    }

    public static void filterOfficialNotices(List<?> posts) {
        if (!hideOfficialNotices()) {
            return;
        }
        if (posts == null || posts.isEmpty()) {
            return;
        }

        try {
            Iterator<?> iterator = posts.iterator();
            while (iterator.hasNext()) {
                if (isOfficialNotice(iterator.next())) {
                    iterator.remove();
                }
            }
        } catch (UnsupportedOperationException ignored) {
        }
    }

    public static boolean isOfficialNotice(Object post) {
        return OFFICIAL_AUTHOR_NAME.equals(getPostAuthorName(post));
    }

    private static boolean isNoticeTab(Object tabType) {
        if (tabType instanceof Enum) {
            return NOTICE_TAB_NAME.equals(((Enum<?>) tabType).name());
        }

        return false;
    }

    private static boolean hideOfficialNotices() {
        try {
            return Settings.hideOfficialNotices();
        } catch (Exception ignored) {
            return true;
        }
    }

    private static String getPostAuthorName(Object post) {
        return null;
    }
}
