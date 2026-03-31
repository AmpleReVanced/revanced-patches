package app.revanced.patches.kakaotalk.settings

import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val addSettingsResourcesPatch = resourcePatch(
    name = "Add settings resources",
    description = "Adds Morphe settings layout resources to the app.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        copyResources(
            "settings",
            ResourceGroup(
                "layout",
                "morphe_kakaotalk_settings.xml"
            ),
            ResourceGroup(
                "xml",
                "morphe_kakaotalk_settings_preferences.xml"
            )
        )
    }
}
