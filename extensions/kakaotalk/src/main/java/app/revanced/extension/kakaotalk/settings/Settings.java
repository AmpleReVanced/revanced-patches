package app.revanced.extension.kakaotalk.settings;

import static java.lang.Boolean.TRUE;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.StringSetting;

/**
 * KakaoTalk-specific Morphe settings, modeled after Morphe's central settings bundles.
 */
@SuppressWarnings("unused")
public final class Settings extends BaseSettings {
    private Settings() {
    }

    public static final BooleanSetting SHOW_MODIFIED_MESSAGE_SENDER_PROFILE =
            new BooleanSetting("kakaotalk_show_modified_message_sender_profile", TRUE);

    public static final BooleanSetting BLOCK_MODIFIED_MESSAGE_REACTION =
            new BooleanSetting("kakaotalk_block_modified_message_reaction", TRUE);

    public static final BooleanSetting BLOCK_MODIFIED_MESSAGE_REPLY =
            new BooleanSetting("kakaotalk_block_modified_message_reply", TRUE);

    public static final BooleanSetting ALLOW_REPLY_TO_FEED =
            new BooleanSetting("kakaotalk_allow_reply_to_feed", TRUE);

    public static final BooleanSetting SHOW_MESSAGE_READ_RECEIPTS =
            new BooleanSetting("kakaotalk_show_message_read_receipts", TRUE);

    public static final BooleanSetting GHOST_MODE =
            new BooleanSetting("kakaotalk_ghost_mode", TRUE);

    public static final BooleanSetting REMOVE_SHORT_FORM_TAB =
            new BooleanSetting("kakaotalk_remove_short_form_tab", TRUE, true);

    public static final BooleanSetting HIDE_MORE_TAB_GAME =
            new BooleanSetting("kakaotalk_hide_more_tab_game", TRUE, true);

    public static final BooleanSetting HIDE_MORE_TAB_KAKAO_PAY_SECTION =
            new BooleanSetting("kakaotalk_hide_more_tab_kakao_pay_section", false, true);

    public static final BooleanSetting HIDE_MORE_TAB_KAKAO_NOW_SECTION =
            new BooleanSetting("kakaotalk_hide_more_tab_kakao_now_section", false, true);

    public static final BooleanSetting HIDE_MORE_TAB_WEATHER_SECTION =
            new BooleanSetting("kakaotalk_hide_more_tab_weather_section", false, true);

    public static final BooleanSetting HIDE_MORE_TAB_SERVICE_GROUP_SECTION =
            new BooleanSetting("kakaotalk_hide_more_tab_service_group_section", false, true);

    public static final BooleanSetting HIDE_MORE_TAB_LINE_SERVICE_SECTION =
            new BooleanSetting("kakaotalk_hide_more_tab_line_service_section", false, true);

    public static final BooleanSetting DISABLE_99_UNREAD_LIMIT =
            new BooleanSetting("kakaotalk_disable_99_unread_limit", TRUE);

    public static final BooleanSetting DEFAULT_EXTERNAL_BROWSER =
            new BooleanSetting("kakaotalk_default_external_browser", TRUE);

    public static final BooleanSetting ENABLE_SEND_BIG_TEXT =
            new BooleanSetting("kakaotalk_enable_send_big_text", false);

    public static final BooleanSetting ENABLE_MARKDOWN =
            new BooleanSetting("kakaotalk_enable_markdown", false);

    public static final BooleanSetting ALLOW_INVISIBLE_CHARACTERS =
            new BooleanSetting("kakaotalk_allow_invisible_characters", false);

    public static final BooleanSetting ALLOW_PROFILE_MEDIA_DOWNLOAD =
            new BooleanSetting("kakaotalk_allow_profile_media_download", TRUE);

    public static final BooleanSetting OPEN_CHAT_ROOM_COMMENT_DISABLED =
            new BooleanSetting("kakaotalk_open_chat_room_comment_disabled", false);

    public static final BooleanSetting BYPASS_MOAT_INTEGRITY_CHECK =
            new BooleanSetting("kakaotalk_bypass_moat_integrity_check", false);

    public static final BooleanSetting ALLOW_OPEN_CHAT_MEDIA_BUNDLE =
            new BooleanSetting("kakaotalk_allow_open_chat_media_bundle", TRUE);

    public static final BooleanSetting STRIP_IMAGE_METADATA =
            new BooleanSetting("kakaotalk_strip_image_metadata", TRUE);

    public static final StringSetting FEATURE_FLAG_OVERRIDES =
            new StringSetting("kakaotalk_feature_flag_overrides", "");

    public static final BooleanSetting FORCE_DEBUG_MODE =
            new BooleanSetting("kakaotalk_force_debug_mode", TRUE, true);

    public static boolean showModifiedMessageSenderProfile() {
        return SHOW_MODIFIED_MESSAGE_SENDER_PROFILE.get();
    }

    public static boolean blockModifiedMessageReaction() {
        return BLOCK_MODIFIED_MESSAGE_REACTION.get();
    }

    public static boolean blockModifiedMessageReply() {
        return BLOCK_MODIFIED_MESSAGE_REPLY.get();
    }

    public static boolean allowReplyToFeed() {
        return ALLOW_REPLY_TO_FEED.get();
    }

    public static boolean showMessageReadReceipts() {
        return SHOW_MESSAGE_READ_RECEIPTS.get();
    }

    public static boolean enableGhostMode() {
        return GHOST_MODE.get();
    }

    public static boolean removeShortFormTab() {
        return REMOVE_SHORT_FORM_TAB.get();
    }

    public static boolean hideMoreTabGame() {
        return HIDE_MORE_TAB_GAME.get();
    }

    public static boolean hideMoreTabKakaoPaySection() {
        return HIDE_MORE_TAB_KAKAO_PAY_SECTION.get();
    }

    public static boolean hideMoreTabKakaoNowSection() {
        return HIDE_MORE_TAB_KAKAO_NOW_SECTION.get();
    }

    public static boolean hideMoreTabWeatherSection() {
        return HIDE_MORE_TAB_WEATHER_SECTION.get();
    }

    public static boolean hideMoreTabServiceGroupSection() {
        return HIDE_MORE_TAB_SERVICE_GROUP_SECTION.get();
    }

    public static boolean hideMoreTabLineServiceSection() {
        return HIDE_MORE_TAB_LINE_SERVICE_SECTION.get();
    }

    public static boolean disable99UnreadLimit() {
        return DISABLE_99_UNREAD_LIMIT.get();
    }

    public static boolean useDefaultExternalBrowser() {
        return DEFAULT_EXTERNAL_BROWSER.get();
    }

    public static boolean enableSendBigText() {
        return ENABLE_SEND_BIG_TEXT.get();
    }

    public static boolean enableMarkdown() {
        return ENABLE_MARKDOWN.get();
    }

    public static boolean allowInvisibleCharacters() {
        return ALLOW_INVISIBLE_CHARACTERS.get();
    }

    public static boolean allowProfileMediaDownload() {
        return ALLOW_PROFILE_MEDIA_DOWNLOAD.get();
    }

    public static boolean openChatRoomCommentDisabled() {
        return OPEN_CHAT_ROOM_COMMENT_DISABLED.get();
    }

    public static boolean bypassMoatIntegrityCheck() {
        return BYPASS_MOAT_INTEGRITY_CHECK.get();
    }

    public static boolean allowOpenChatMediaBundle() {
        return ALLOW_OPEN_CHAT_MEDIA_BUNDLE.get();
    }

    public static boolean stripImageMetadata() {
        return STRIP_IMAGE_METADATA.get();
    }

    public static String featureFlagOverrides() {
        return FEATURE_FLAG_OVERRIDES.get();
    }

    public static boolean forceDebugMode() {
        return FORCE_DEBUG_MODE.get();
    }
}
