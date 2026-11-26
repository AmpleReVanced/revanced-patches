package app.revanced.patches.dcinside.layout.posticons

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val SETTINGS_CLASS = "Lapp/revanced/extension/dcinside/settings/Settings;"
private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/RestoreOldPostIconsPatch;"

private val postIconDrawableNames = listOf(
    "ic_list_head_best",
    "ic_list_head_image",
    "ic_list_head_image_tinted",
    "ic_list_head_recommend",
    "ic_list_head_recommend_tinted0",
    "ic_list_head_recommend_tinted1",
    "ic_list_head_recommend_tinted2",
    "ic_list_head_video",
    "ic_list_head_video_tinted",
    "ic_read_like_normal",
    "ic_read_like_normal_tinted",
    "ic_read_like_recommend",
    "ic_read_like_recommend_tinted",
)

internal val restoreOldPostIconsResourcesPatch = patch {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        copyResources(
            "dcinside/legacy-icons",
            ResourceGroup(
                "drawable-xxhdpi",
                "morphe_legacy_post_icon_image.webp",
                "morphe_legacy_post_icon_recommend_image.webp",
                "morphe_legacy_post_icon_recommend_text.webp",
                "morphe_legacy_post_icon_recommend_video.webp",
                "morphe_legacy_post_icon_video.png",
                "morphe_legacy_read_like_normal.webp",
                "morphe_legacy_read_like_recommend.webp",
            ),
        )
    }
}

@Suppress("unused")
val restoreOldPostIconsPatch = patch(
    name = "Restore old post icons",
    description = "Adds a setting to restore DCInside 4.7.x post list and read footer icons.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(
        addSettingsPatch,
        restoreOldPostIconsResourcesPatch,
    )

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            SwitchPreference(
                key = "morphe_pref_restore_legacy_post_icons",
                titleKey = "morphe_settings_restore_legacy_post_icons",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        var patchedInstructions = 0
        postIconDrawableNames.forEach { name ->
            resourceLiteral(ResourceType.DRAWABLE, name).matchAllMethodIndicesForEach(requireMatches = false) { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstructions(
                    index + 1,
                    """
                        invoke-static/range {v$register .. v$register}, $SETTINGS_CLASS->getLegacyPostIconResource(I)I
                        move-result v$register
                    """,
                )
                patchedInstructions++
            }
        }

        if (patchedInstructions == 0) {
            throw PatchException("Could not find DCInside post icon resource references")
        }
    }
}
