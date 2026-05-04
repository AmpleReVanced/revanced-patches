package app.revanced.extension.dcinside.settings;

import static java.lang.Boolean.TRUE;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;

@SuppressWarnings("unused")
public final class Settings extends BaseSettings {
    private Settings() {
    }

    public static final BooleanSetting HIDE_OFFICIAL_NOTICES =
            new BooleanSetting("dcinside_hide_official_notices", TRUE);
    public static final BooleanSetting BLOCK_POST_DCCON_LOADING =
            new BooleanSetting("dcinside_block_post_dccon_loading", TRUE);
    public static final BooleanSetting BLOCK_REPLY_DCCON_LOADING =
            new BooleanSetting("dcinside_block_reply_dccon_loading", TRUE);

    public static boolean hideOfficialNotices() {
        return HIDE_OFFICIAL_NOTICES.get();
    }

    public static boolean blockPostDcconLoading() {
        return BLOCK_POST_DCCON_LOADING.get();
    }

    public static boolean blockReplyDcconLoading() {
        return BLOCK_REPLY_DCCON_LOADING.get();
    }

    public static boolean isDcconUrl(String url) {
        return url != null && url.contains("dccon.php");
    }
}
