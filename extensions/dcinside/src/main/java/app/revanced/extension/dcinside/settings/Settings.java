package app.revanced.extension.dcinside.settings;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.FALSE;

import android.view.View;
import android.view.ViewGroup;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.revanced.extension.dcinside.helper.ResourceHelper;

@SuppressWarnings("unused")
public final class Settings extends BaseSettings {
    private static final int NORMAL_DCCON_SIZE_DP = 100;

    private Settings() {
    }

    public static final BooleanSetting HIDE_OFFICIAL_NOTICES =
            new BooleanSetting("dcinside_hide_official_notices", TRUE);
    public static final BooleanSetting BLOCK_POST_DCCON_LOADING =
            new BooleanSetting("dcinside_block_post_dccon_loading", FALSE);
    public static final BooleanSetting BLOCK_REPLY_DCCON_LOADING =
            new BooleanSetting("dcinside_block_reply_dccon_loading", FALSE);
    public static final BooleanSetting RENDER_BIG_DCCON_AS_NORMAL =
            new BooleanSetting("dcinside_render_big_dccon_as_normal", FALSE);
    public static final BooleanSetting HIDE_MINI_GALLERY_COVER_IMAGE =
            new BooleanSetting("dcinside_hide_mini_gallery_cover_image", FALSE);
    public static final BooleanSetting RESTORE_LEGACY_POST_ICONS =
            new BooleanSetting("dcinside_restore_legacy_post_icons", FALSE);
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

    private static boolean legacyPostIconResourceIdsLoaded;
    private static int listHeadBestIcon;
    private static int listHeadImageIcon;
    private static int listHeadImageTintedIcon;
    private static int listHeadRecommendIcon;
    private static int listHeadRecommendImageIcon;
    private static int listHeadRecommendTextIcon;
    private static int listHeadRecommendVideoIcon;
    private static int listHeadVideoIcon;
    private static int listHeadVideoTintedIcon;
    private static int readLikeNormalIcon;
    private static int readLikeNormalTintedIcon;
    private static int readLikeRecommendIcon;
    private static int readLikeRecommendTintedIcon;
    private static int legacyPostImageIcon;
    private static int legacyPostRecommendImageIcon;
    private static int legacyPostRecommendTextIcon;
    private static int legacyPostRecommendVideoIcon;
    private static int legacyPostVideoIcon;
    private static int legacyReadLikeNormalIcon;
    private static int legacyReadLikeRecommendIcon;

    public static boolean hideOfficialNotices() {
        return HIDE_OFFICIAL_NOTICES.get();
    }

    public static boolean blockPostDcconLoading() {
        return BLOCK_POST_DCCON_LOADING.get();
    }

    public static boolean blockReplyDcconLoading() {
        return BLOCK_REPLY_DCCON_LOADING.get();
    }

    public static boolean renderBigDcconAsNormal() {
        return RENDER_BIG_DCCON_AS_NORMAL.get();
    }

    public static boolean hideMiniGalleryCoverImage() {
        return HIDE_MINI_GALLERY_COVER_IMAGE.get();
    }

    public static boolean restoreLegacyPostIcons() {
        return RESTORE_LEGACY_POST_ICONS.get();
    }

    public static int getLegacyPostIconResource(int resourceId) {
        if (!restoreLegacyPostIcons()) {
            return resourceId;
        }

        loadLegacyPostIconResourceIds();

        if (isAnyResource(resourceId, listHeadImageIcon, listHeadImageTintedIcon)) {
            return replacementOrOriginal(legacyPostImageIcon, resourceId);
        }
        if (isAnyResource(resourceId, listHeadVideoIcon, listHeadVideoTintedIcon)) {
            return replacementOrOriginal(legacyPostVideoIcon, resourceId);
        }
        if (isAnyResource(resourceId, listHeadRecommendIcon, listHeadRecommendImageIcon)) {
            return replacementOrOriginal(legacyPostRecommendImageIcon, resourceId);
        }
        if (isResource(resourceId, listHeadRecommendTextIcon)) {
            return replacementOrOriginal(legacyPostRecommendTextIcon, resourceId);
        }
        if (isAnyResource(resourceId, listHeadRecommendVideoIcon, listHeadBestIcon)) {
            return replacementOrOriginal(legacyPostRecommendVideoIcon, resourceId);
        }
        if (isAnyResource(resourceId, readLikeNormalIcon, readLikeNormalTintedIcon)) {
            return replacementOrOriginal(legacyReadLikeNormalIcon, resourceId);
        }
        if (isAnyResource(resourceId, readLikeRecommendIcon, readLikeRecommendTintedIcon)) {
            return replacementOrOriginal(legacyReadLikeRecommendIcon, resourceId);
        }

        return resourceId;
    }

    private static void loadLegacyPostIconResourceIds() {
        if (legacyPostIconResourceIdsLoaded) {
            return;
        }

        legacyPostIconResourceIdsLoaded = true;
        listHeadBestIcon = drawableId("ic_list_head_best");
        listHeadImageIcon = drawableId("ic_list_head_image");
        listHeadImageTintedIcon = drawableId("ic_list_head_image_tinted");
        listHeadRecommendIcon = drawableId("ic_list_head_recommend");
        listHeadRecommendImageIcon = drawableId("ic_list_head_recommend_tinted0");
        listHeadRecommendTextIcon = drawableId("ic_list_head_recommend_tinted1");
        listHeadRecommendVideoIcon = drawableId("ic_list_head_recommend_tinted2");
        listHeadVideoIcon = drawableId("ic_list_head_video");
        listHeadVideoTintedIcon = drawableId("ic_list_head_video_tinted");
        readLikeNormalIcon = drawableId("ic_read_like_normal");
        readLikeNormalTintedIcon = drawableId("ic_read_like_normal_tinted");
        readLikeRecommendIcon = drawableId("ic_read_like_recommend");
        readLikeRecommendTintedIcon = drawableId("ic_read_like_recommend_tinted");

        legacyPostImageIcon = drawableId("morphe_legacy_post_icon_image");
        legacyPostRecommendImageIcon = drawableId("morphe_legacy_post_icon_recommend_image");
        legacyPostRecommendTextIcon = drawableId("morphe_legacy_post_icon_recommend_text");
        legacyPostRecommendVideoIcon = drawableId("morphe_legacy_post_icon_recommend_video");
        legacyPostVideoIcon = drawableId("morphe_legacy_post_icon_video");
        legacyReadLikeNormalIcon = drawableId("morphe_legacy_read_like_normal");
        legacyReadLikeRecommendIcon = drawableId("morphe_legacy_read_like_recommend");
    }

    private static int drawableId(String name) {
        return ResourceHelper.getResourceId("drawable", name);
    }

    private static boolean isAnyResource(int resourceId, int first, int second) {
        return isResource(resourceId, first) || isResource(resourceId, second);
    }

    private static boolean isResource(int resourceId, int candidate) {
        return candidate != 0 && resourceId == candidate;
    }

    private static int replacementOrOriginal(int replacement, int original) {
        return replacement == 0 ? original : replacement;
    }

    public static void resizeReplyDcconAsNormal(View view) {
        if (!renderBigDcconAsNormal() || view == null) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            return;
        }

        int size = (int) (NORMAL_DCCON_SIZE_DP * view.getResources().getDisplayMetrics().density + 0.5f);
        if (layoutParams.width == size && layoutParams.height == size) {
            return;
        }

        layoutParams.width = size;
        layoutParams.height = size;
        view.setLayoutParams(layoutParams);
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
