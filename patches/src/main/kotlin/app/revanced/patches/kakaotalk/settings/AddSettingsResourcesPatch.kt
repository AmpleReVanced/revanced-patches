package app.revanced.patches.kakaotalk.settings

import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.addKakaoTalkResources

@Suppress("unused")
val addSettingsResourcesPatch = resourcePatch(
    name = "Add settings resources",
    description = "Adds Morphe settings layout resources to the app.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        addKakaoTalkResources()

        copyResources(
            "settings",
            ResourceGroup(
                "drawable",
                "morphe_settings_icon_dynamic.xml",
                "morphe_settings_icon_light.xml",
                "morphe_settings_icon_dark.xml",
                "morphe_settings_icon_bold_light.xml",
                "morphe_settings_icon_bold_dark.xml",
                "morphe_pref_icon_chat.xml",
                "morphe_pref_icon_navigation.xml",
                "morphe_pref_icon_features.xml",
                "morphe_pref_icon_support.xml",
                "morphe_pref_icon_information.xml",
                "morphe_pref_icon_debug.xml",
                "morphe_pref_icon_manage.xml",
                "morphe_pref_icon_links.xml",
            ),
            ResourceGroup(
                "layout",
                "morphe_kakaotalk_settings.xml",
                "morphe_preference_screen.xml",
            ),
            ResourceGroup(
                "xml",
                "morphe_kakaotalk_settings_preferences.xml",
            ),
        )

        addDefaultKakaoTalkSettingsPreferences()
    }

    finalize {
        PreferenceScreen.close()

        document("res/xml/morphe_kakaotalk_settings_preferences.xml").use { document ->
            val preferenceScreen = document.getElementsByTagName("PreferenceScreen").item(0)
                ?: throw IllegalStateException("Missing PreferenceScreen root")

            while (preferenceScreen.hasChildNodes()) {
                preferenceScreen.removeChild(preferenceScreen.firstChild)
            }

            orderedKakaoTalkSettingsPreferences().forEach { preference ->
                preferenceScreen.appendChild(preference.serialize(document) { })
                preferenceScreen.appendChild(document.createTextNode("\n"))
            }
        }
    }
}
