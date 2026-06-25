package app.revanced.patches.kakaotalk.settings

import app.morphe.patches.shared.misc.settings.preference.ActionIntentPreference
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.NonInteractivePreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference

private const val ACTION_VIEW = "android.intent.action.VIEW"
private const val ACTION_SENDTO = "android.intent.action.SENDTO"

internal val kakaoTalkSettingsPreferences = linkedSetOf<PreferenceScreenPreference>()

private val kakaoTalkSettingsPreferenceOrder = listOf(
    "morphe_pref_category_chat",
    "morphe_pref_category_navigation",
    "morphe_pref_category_features",
    "morphe_pref_category_support",
    "morphe_pref_category_information",
    "morphe_pref_category_debug",
    "morphe_pref_category_manage",
    "morphe_pref_category_links",
)

private var defaultPreferencesAdded = false

internal fun addDefaultKakaoTalkSettingsPreferences() {
    if (defaultPreferencesAdded) return
    defaultPreferencesAdded = true

    PreferenceScreen.SUPPORT.addPreferences(
        ActionIntentPreference(
            key = "morphe_pref_support",
            titleKey = "morphe_settings_support",
            summaryKey = "morphe_settings_support_summary",
            intent = ActionIntentPreference.Intent(
                action = ACTION_SENDTO,
                data = "mailto:thisisample@proton.me?subject=Morphe%20support",
            ),
        ),
    )

    PreferenceScreen.INFORMATION.addPreferences(
        NonInteractivePreference(
            key = "morphe_pref_app_version",
            titleKey = "morphe_settings_app_version",
            summaryKey = null,
        ),
        NonInteractivePreference(
            key = "morphe_pref_patches_version",
            titleKey = "morphe_settings_patches_version",
            summaryKey = null,
        ),
        NonInteractivePreference(
            key = "morphe_pref_package_name",
            titleKey = "morphe_settings_package_name",
            summaryKey = null,
        ),
    )

    PreferenceScreen.DEBUG.addPreferences(
        SwitchPreference(
            key = "morphe_pref_debug",
            titleKey = "morphe_settings_debug",
            summary = true,
        ),
        SwitchPreference(
            key = "morphe_pref_debug_stacktrace",
            titleKey = "morphe_settings_debug_stacktrace",
            summary = true,
        ),
        SwitchPreference(
            key = "morphe_pref_debug_toast",
            titleKey = "morphe_settings_debug_toast",
            summary = true,
        ),
    )

    PreferenceScreen.MANAGE.addPreferences(
        NonInteractivePreference(
            key = "morphe_pref_reset",
            titleKey = "morphe_settings_reset_all",
            summaryKey = "morphe_settings_reset_all_summary",
            selectable = true,
        ),
    )

    PreferenceScreen.LINKS.addPreferences(
        ActionIntentPreference(
            key = "morphe_pref_github",
            titleKey = "morphe_settings_link_github",
            summaryKey = "morphe_settings_link_github_summary",
            intent = ActionIntentPreference.Intent(
                action = ACTION_VIEW,
                data = "https://github.com/AmpleReVanced/revanced-patches",
            ),
        ),
        ActionIntentPreference(
            key = "morphe_pref_telegram",
            titleKey = "morphe_settings_link_telegram",
            summaryKey = "morphe_settings_link_telegram_summary",
            intent = ActionIntentPreference.Intent(
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
        summaryKey = null,
        sorting = Sorting.UNSORTED,
    )
    val NAVIGATION = Screen(
        key = "morphe_pref_category_navigation",
        titleKey = "morphe_settings_section_navigation",
        summaryKey = null,
        sorting = Sorting.UNSORTED,
    )
    val FEATURES = Screen(
        key = "morphe_pref_category_features",
        titleKey = "morphe_settings_section_features",
        summaryKey = null,
        sorting = Sorting.UNSORTED,
    )
    val SUPPORT = Screen(
        key = "morphe_pref_category_support",
        titleKey = "morphe_settings_section_support",
        summaryKey = null,
        sorting = Sorting.UNSORTED,
    )
    val INFORMATION = Screen(
        key = "morphe_pref_category_information",
        titleKey = "morphe_settings_section_information",
        summaryKey = null,
        sorting = Sorting.UNSORTED,
    )
    val DEBUG = Screen(
        key = "morphe_pref_category_debug",
        titleKey = "morphe_settings_section_debug",
        summaryKey = null,
        sorting = Sorting.UNSORTED,
    )
    val MANAGE = Screen(
        key = "morphe_pref_category_manage",
        titleKey = "morphe_settings_section_manage",
        summaryKey = null,
        sorting = Sorting.UNSORTED,
    )
    val LINKS = Screen(
        key = "morphe_pref_category_links",
        titleKey = "morphe_settings_section_links",
        summaryKey = null,
        sorting = Sorting.UNSORTED,
    )

    override fun commit(screen: PreferenceScreenPreference) {
        kakaoTalkSettingsPreferences += screen
    }
}

internal fun orderedKakaoTalkSettingsPreferences(): List<PreferenceScreenPreference> =
    kakaoTalkSettingsPreferences.sortedBy { preference ->
        kakaoTalkSettingsPreferenceOrder.indexOf(preference.key).let { index ->
            if (index < 0) Int.MAX_VALUE else index
        }
    }
