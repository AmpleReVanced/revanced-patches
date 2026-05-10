package app.revanced.extension.dcinside.settings;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.FALSE;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;

@SuppressWarnings("unused")
public final class Settings extends BaseSettings {
    private Settings() {
    }

    public static final BooleanSetting HIDE_OFFICIAL_NOTICES =
            new BooleanSetting("dcinside_hide_official_notices", TRUE);
    public static final BooleanSetting BLOCK_POST_DCCON_LOADING =
            new BooleanSetting("dcinside_block_post_dccon_loading", FALSE);
    public static final BooleanSetting BLOCK_REPLY_DCCON_LOADING =
            new BooleanSetting("dcinside_block_reply_dccon_loading", FALSE);
    public static final BooleanSetting HIDE_HOME_SEARCH_MENU =
            new BooleanSetting("dcinside_hide_home_search_menu", FALSE);
    public static final BooleanSetting HIDE_HOME_RECENT_GALLERIES =
            new BooleanSetting("dcinside_hide_home_recent_galleries", FALSE);
    public static final BooleanSetting HIDE_HOME_RECOMMENDED_GALLERIES =
            new BooleanSetting("dcinside_hide_home_recommended_galleries", FALSE);
    public static final BooleanSetting HIDE_HOME_GALLERY_RANKING =
            new BooleanSetting("dcinside_hide_home_gallery_ranking", FALSE);
    public static final BooleanSetting HIDE_HOME_LIVE_BEST =
            new BooleanSetting("dcinside_hide_home_live_best", FALSE);
    public static final BooleanSetting HIDE_HOME_RECOMMENDED_POSTS =
            new BooleanSetting("dcinside_hide_home_recommended_posts", FALSE);

    public static boolean hideOfficialNotices() {
        return HIDE_OFFICIAL_NOTICES.get();
    }

    public static boolean blockPostDcconLoading() {
        return BLOCK_POST_DCCON_LOADING.get();
    }

    public static boolean blockReplyDcconLoading() {
        return BLOCK_REPLY_DCCON_LOADING.get();
    }

    public static boolean hideHomeSearchMenu() {
        return HIDE_HOME_SEARCH_MENU.get();
    }

    public static boolean hideHomeRecentGalleries() {
        return HIDE_HOME_RECENT_GALLERIES.get();
    }

    public static boolean hideHomeRecommendedGalleries() {
        return HIDE_HOME_RECOMMENDED_GALLERIES.get();
    }

    public static boolean hideHomeGalleryRanking() {
        return HIDE_HOME_GALLERY_RANKING.get();
    }

    public static boolean hideHomeLiveBest() {
        return HIDE_HOME_LIVE_BEST.get();
    }

    public static boolean hideHomeRecommendedPosts() {
        return HIDE_HOME_RECOMMENDED_POSTS.get();
    }

    public static boolean hideAnyHomeComponent() {
        return hideHomeSearchMenu()
                || hideHomeRecentGalleries()
                || hideHomeRecommendedGalleries()
                || hideHomeGalleryRanking()
                || hideHomeLiveBest()
                || hideHomeRecommendedPosts();
    }

    public static boolean isDcconUrl(String url) {
        return url != null && url.contains("dccon.php");
    }
}
