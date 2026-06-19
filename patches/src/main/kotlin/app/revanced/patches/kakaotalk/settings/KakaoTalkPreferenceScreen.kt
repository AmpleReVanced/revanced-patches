package app.revanced.patches.kakaotalk.settings

import app.revanced.patches.kakaotalk.settings.preference.BasePreference
import app.revanced.patches.kakaotalk.settings.preference.BasePreferenceScreen
import app.revanced.patches.kakaotalk.settings.preference.EditTextPreference
import app.revanced.patches.kakaotalk.settings.preference.IntentPreference
import app.revanced.patches.kakaotalk.settings.preference.SwitchPreference
import app.revanced.patches.kakaotalk.settings.preference.TextPreference

private const val ACTION_VIEW = "android.intent.action.VIEW"

internal val kakaoTalkSettingsPreferences = linkedSetOf<BasePreference>()

private var defaultPreferencesAdded = false

internal fun addDefaultKakaoTalkSettingsPreferences() {
    if (defaultPreferencesAdded) return
    defaultPreferencesAdded = true

    PreferenceScreen.CHAT.addPreferences(
        SwitchPreference(
            key = "morphe_pref_ghost_mode",
            titleKey = "morphe_settings_patch_ghost_mode",
            summaryKey = "morphe_settings_patch_ghost_mode_summary",
            defaultValue = true,
        ),
        SwitchPreference(
            key = "morphe_pref_disable_99_unread_limit",
            titleKey = "morphe_settings_patch_disable_99_unread_limit",
            summaryKey = "morphe_settings_patch_disable_99_unread_limit_summary",
            defaultValue = true,
        ),
        SwitchPreference(
            key = "morphe_pref_open_chat_room_comment_disabled",
            titleKey = "morphe_settings_patch_open_chat_room_comment_disabled",
            summaryKey = "morphe_settings_patch_open_chat_room_comment_disabled_summary",
            defaultValue = false,
        ),
        EditTextPreference(
            key = "morphe_pref_feature_flag_overrides",
            titleKey = "morphe_settings_patch_feature_flag_overrides",
            summaryKey = "morphe_settings_patch_feature_flag_overrides_summary",
        ),
    )

    PreferenceScreen.NAVIGATION.addPreferences(
        SwitchPreference(
            key = "morphe_pref_remove_short_form_tab",
            titleKey = "morphe_settings_catalog_remove_short_form_tab",
            summaryKey = "morphe_settings_catalog_remove_short_form_tab_summary",
            defaultValue = true,
        ),
        SwitchPreference(
            key = "morphe_pref_hide_more_tab_game",
            titleKey = "morphe_settings_catalog_hide_more_tab_game",
            summaryKey = "morphe_settings_catalog_hide_more_tab_game_summary",
            defaultValue = true,
        ),
        SwitchPreference(
            key = "morphe_pref_hide_more_tab_kakao_pay_section",
            titleKey = "morphe_settings_catalog_hide_more_tab_kakao_pay_section",
            summaryKey = "morphe_settings_catalog_hide_more_tab_kakao_pay_section_summary",
            defaultValue = false,
        ),
        SwitchPreference(
            key = "morphe_pref_hide_more_tab_game_play_section",
            titleKey = "morphe_settings_catalog_hide_more_tab_game_play_section",
            summaryKey = "morphe_settings_catalog_hide_more_tab_game_play_section_summary",
            defaultValue = false,
        ),
        SwitchPreference(
            key = "morphe_pref_hide_more_tab_kakao_now_section",
            titleKey = "morphe_settings_catalog_hide_more_tab_kakao_now_section",
            summaryKey = "morphe_settings_catalog_hide_more_tab_kakao_now_section_summary",
            defaultValue = false,
        ),
        SwitchPreference(
            key = "morphe_pref_hide_more_tab_weather_section",
            titleKey = "morphe_settings_catalog_hide_more_tab_weather_section",
            summaryKey = "morphe_settings_catalog_hide_more_tab_weather_section_summary",
            defaultValue = false,
        ),
        SwitchPreference(
            key = "morphe_pref_hide_more_tab_service_group_section",
            titleKey = "morphe_settings_catalog_hide_more_tab_service_group_section",
            summaryKey = "morphe_settings_catalog_hide_more_tab_service_group_section_summary",
            defaultValue = false,
        ),
        SwitchPreference(
            key = "morphe_pref_hide_more_tab_line_service_section",
            titleKey = "morphe_settings_catalog_hide_more_tab_line_service_section",
            summaryKey = "morphe_settings_catalog_hide_more_tab_line_service_section_summary",
            defaultValue = false,
        ),
    )

    PreferenceScreen.FEATURES.addPreferences(
        SwitchPreference(
            key = "morphe_pref_default_external_browser",
            titleKey = "morphe_settings_patch_external_browser",
            summaryKey = "morphe_settings_patch_external_browser_summary",
            defaultValue = true,
        ),
        SwitchPreference(
            key = "morphe_pref_enable_recording_pause_resume",
            titleKey = "morphe_settings_patch_recording_pause_resume",
            summaryKey = "morphe_settings_patch_recording_pause_resume_summary",
            defaultValue = true,
        ),
        SwitchPreference(
            key = "morphe_pref_enable_send_big_text",
            titleKey = "morphe_settings_patch_send_big_text",
            summaryKey = "morphe_settings_patch_send_big_text_summary",
            defaultValue = false,
        ),
        SwitchPreference(
            key = "morphe_pref_enable_markdown",
            titleKey = "morphe_settings_patch_markdown",
            summaryKey = "morphe_settings_patch_markdown_summary",
            defaultValue = false,
        ),
        SwitchPreference(
            key = "morphe_pref_play_youtube_player_in_chat_room",
            titleKey = "morphe_settings_patch_play_youtube_player_in_chat_room",
            summaryKey = "morphe_settings_patch_play_youtube_player_in_chat_room_summary",
            defaultValue = true,
        ),
        SwitchPreference(
            key = "morphe_pref_bypass_moat_integrity_check",
            titleKey = "morphe_settings_patch_bypass_moat_check",
            summaryKey = "morphe_settings_patch_bypass_moat_check_summary",
            defaultValue = false,
        ),
    )

    PreferenceScreen.INFORMATION.addPreferences(
        TextPreference(
            key = "morphe_pref_app_version",
            titleKey = "morphe_settings_app_version",
        ),
        TextPreference(
            key = "morphe_pref_patches_version",
            titleKey = "morphe_settings_patches_version",
        ),
        TextPreference(
            key = "morphe_pref_package_name",
            titleKey = "morphe_settings_package_name",
        ),
    )

    PreferenceScreen.DEBUG.addPreferences(
        SwitchPreference(
            key = "morphe_pref_force_debug_mode",
            titleKey = "morphe_settings_patch_force_debug_mode",
            summaryKey = "morphe_settings_patch_force_debug_mode_summary",
            defaultValue = true,
        ),
        SwitchPreference(
            key = "morphe_pref_debug",
            titleKey = "morphe_settings_debug",
            summaryKey = "morphe_settings_debug_summary",
            defaultValue = false,
        ),
        SwitchPreference(
            key = "morphe_pref_debug_stacktrace",
            titleKey = "morphe_settings_debug_stacktrace",
            summaryKey = "morphe_settings_debug_stacktrace_summary",
            defaultValue = false,
            dependency = "morphe_pref_debug",
        ),
        SwitchPreference(
            key = "morphe_pref_debug_toast",
            titleKey = "morphe_settings_debug_toast",
            summaryKey = "morphe_settings_debug_toast_summary",
            defaultValue = true,
        ),
    )

    PreferenceScreen.MANAGE.addPreferences(
        TextPreference(
            key = "morphe_pref_reset",
            titleKey = "morphe_settings_reset_all",
            summaryKey = "morphe_settings_reset_all_summary",
        ),
    )

    PreferenceScreen.LINKS.addPreferences(
        IntentPreference(
            key = "morphe_pref_github",
            titleKey = "morphe_settings_link_github",
            summaryKey = "morphe_settings_link_github_summary",
            intent = IntentPreference.Intent(
                action = ACTION_VIEW,
                data = "https://github.com/AmpleReVanced/revanced-patches",
            ),
        ),
        IntentPreference(
            key = "morphe_pref_telegram",
            titleKey = "morphe_settings_link_telegram",
            summaryKey = "morphe_settings_link_telegram_summary",
            intent = IntentPreference.Intent(
                action = ACTION_VIEW,
                data = "https://t.me/ample_revanced_bot",
            ),
        ),
    )
}

internal object PreferenceScreen : BasePreferenceScreen() {
    val CHAT = Screen(
        key = "morphe_pref_category_chat",
        titleKey = "morphe_settings_section_chat",
    )
    val NAVIGATION = Screen(
        key = "morphe_pref_category_navigation",
        titleKey = "morphe_settings_section_navigation",
    )
    val FEATURES = Screen(
        key = "morphe_pref_category_features",
        titleKey = "morphe_settings_section_features",
    )
    val INFORMATION = Screen(
        key = "morphe_pref_category_information",
        titleKey = "morphe_settings_section_information",
    )
    val DEBUG = Screen(
        key = "morphe_pref_category_debug",
        titleKey = "morphe_settings_section_debug",
    )
    val MANAGE = Screen(
        key = "morphe_pref_category_manage",
        titleKey = "morphe_settings_section_manage",
    )
    val LINKS = Screen(
        key = "morphe_pref_category_links",
        titleKey = "morphe_settings_section_links",
    )

    override fun commit(preference: BasePreference) {
        kakaoTalkSettingsPreferences += preference
    }
}
