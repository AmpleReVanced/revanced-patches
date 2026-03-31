package app.revanced.extension.kakaotalk.settings;

import static java.lang.Boolean.TRUE;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;

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

    public static final BooleanSetting FORCE_DEBUG_MODE =
            new BooleanSetting("kakaotalk_force_debug_mode", TRUE, true);

    public static boolean showDeletedHiddenMessages() {
        return SHOW_DELETED_HIDDEN_MESSAGES.get();
    }

    public static boolean enableGhostMode() {
        return GHOST_MODE.get();
    }

    public static boolean forceDebugMode() {
        return FORCE_DEBUG_MODE.get();
    }
}
