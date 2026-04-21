package app.revanced.extension.kakaotalk.settings;

import static java.lang.Boolean.TRUE;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;

/**
 * KakaoTalk-specific Morphe settings, modeled after Morphe's central settings bundles.
 */
@SuppressWarnings("unused")
public final class Settings extends BaseSettings {
    private Settings() {
    }

    public static final BooleanSetting SHOW_DELETED_HIDDEN_MESSAGES =
            new BooleanSetting("kakaotalk_show_deleted_hidden_messages", TRUE);

    public static final BooleanSetting GHOST_MODE =
            new BooleanSetting("kakaotalk_ghost_mode", TRUE);

    public static final BooleanSetting REMOVE_SHORT_FORM_TAB =
            new BooleanSetting("kakaotalk_remove_short_form_tab", TRUE, true);

    public static final BooleanSetting DISABLE_99_UNREAD_LIMIT =
            new BooleanSetting("kakaotalk_disable_99_unread_limit", TRUE);

    public static final BooleanSetting DEFAULT_EXTERNAL_BROWSER =
            new BooleanSetting("kakaotalk_default_external_browser", TRUE);

    public static final BooleanSetting ENABLE_RECORDING_PAUSE_RESUME =
            new BooleanSetting("kakaotalk_enable_recording_pause_resume", TRUE);

    public static final BooleanSetting ENABLE_SEND_BIG_TEXT =
            new BooleanSetting("kakaotalk_enable_send_big_text", false);

    public static final BooleanSetting ENABLE_MARKDOWN =
            new BooleanSetting("kakaotalk_enable_markdown", false);

    public static final BooleanSetting PLAY_YOUTUBE_PLAYER_IN_CHAT_ROOM =
            new BooleanSetting("kakaotalk_play_youtube_player_in_chat_room", TRUE);

    public static final BooleanSetting FORCE_DEBUG_MODE =
            new BooleanSetting("kakaotalk_force_debug_mode", TRUE, true);

    public static boolean showDeletedHiddenMessages() {
        return SHOW_DELETED_HIDDEN_MESSAGES.get();
    }

    public static boolean enableGhostMode() {
        return GHOST_MODE.get();
    }

    public static boolean removeShortFormTab() {
        return REMOVE_SHORT_FORM_TAB.get();
    }

    public static boolean disable99UnreadLimit() {
        return DISABLE_99_UNREAD_LIMIT.get();
    }

    public static boolean useDefaultExternalBrowser() {
        return DEFAULT_EXTERNAL_BROWSER.get();
    }

    public static boolean enableRecordingPauseResume() {
        return ENABLE_RECORDING_PAUSE_RESUME.get();
    }

    public static boolean enableSendBigText() {
        return ENABLE_SEND_BIG_TEXT.get();
    }

    public static boolean enableMarkdown() {
        return ENABLE_MARKDOWN.get();
    }

    public static boolean playYoutubePlayerInChatRoom() {
        return PLAY_YOUTUBE_PLAYER_IN_CHAT_ROOM.get();
    }

    public static boolean forceDebugMode() {
        return FORCE_DEBUG_MODE.get();
    }
}
