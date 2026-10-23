package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabGlobalServiceGroupSectionFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabKakaoNowSectionFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabKakaoPayExperimentFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabKakaoPaySectionFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabLineGridServiceSectionFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabLineServiceSectionFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabListAdapterFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabServiceGroupSectionFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabWeatherSectionFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.ListFilterGroup
import app.revanced.patches.kakaotalk.shared.filterAdapterItems
import app.revanced.patches.kakaotalk.shared.setListFilter

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/HideMoreTabComponentsPatch;"

@Suppress("unused")
val hideMoreTabComponentsPatch = bytecodePatch(
    name = "Hide More tab components",
    description = "Adds options to hide components from the More tab.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.NAVIGATION.addPreferences(
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_kakao_pay_section",
                titleKey = "morphe_settings_catalog_hide_more_tab_kakao_pay_section",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_kakao_now_section",
                titleKey = "morphe_settings_catalog_hide_more_tab_kakao_now_section",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_weather_section",
                titleKey = "morphe_settings_catalog_hide_more_tab_weather_section",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_service_group_section",
                titleKey = "morphe_settings_catalog_hide_more_tab_service_group_section",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_line_service_section",
                titleKey = "morphe_settings_catalog_hide_more_tab_line_service_section",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        setListFilter(
            EXTENSION_CLASS,
            listOf(
                ListFilterGroup(
                    listOf(
                        MoreTabKakaoPaySectionFingerprint.originalClassDef.type,
                        MoreTabKakaoPayExperimentFingerprint.originalClassDef.type,
                    ),
                    listOf("hideMoreTabKakaoPaySection"),
                ),
                ListFilterGroup(
                    listOf(MoreTabKakaoNowSectionFingerprint.originalClassDef.type),
                    listOf("hideMoreTabKakaoNowSection"),
                ),
                ListFilterGroup(
                    listOf(MoreTabWeatherSectionFingerprint.originalClassDef.type),
                    listOf("hideMoreTabWeatherSection"),
                ),
                ListFilterGroup(
                    listOf(
                        MoreTabServiceGroupSectionFingerprint.originalClassDef.type,
                        MoreTabGlobalServiceGroupSectionFingerprint.originalClassDef.type,
                    ),
                    listOf("hideMoreTabServiceGroupSection"),
                ),
                ListFilterGroup(
                    listOf(
                        MoreTabLineServiceSectionFingerprint.originalClassDef.type,
                        MoreTabLineGridServiceSectionFingerprint.originalClassDef.type,
                    ),
                    listOf("hideMoreTabServiceGroupSection", "hideMoreTabLineServiceSection"),
                ),
            ),
        )
        filterAdapterItems(MoreTabListAdapterFingerprint.classDef, EXTENSION_CLASS)
    }
}
