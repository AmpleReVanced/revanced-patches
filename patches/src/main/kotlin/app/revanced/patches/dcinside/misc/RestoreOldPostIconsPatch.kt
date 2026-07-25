package app.revanced.patches.dcinside.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.findMutableMethodOf
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.settings.PreferenceScreen
import app.revanced.patches.dcinside.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
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

internal val restoreOldPostIconsResourcesPatch = resourcePatch {
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
val restoreOldPostIconsPatch = bytecodePatch(
    name = "Restore old post icons",
    description = "Adds a setting to restore DCInside 4.7.x post list and read footer icons.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(
        addSettingsPatch,
        restoreOldPostIconsResourcesPatch,
        resourceMappingPatch,
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

        val resourceIds = drawableResourceIds(postIconDrawableNames)
        var patchedInstructions = 0

        classDefForEach { classDef ->
            val methods = classDef.methods
                .filter { method -> method.hasLegacyPostIconResourceLoad(resourceIds) }

            if (methods.isNotEmpty()) {
                val mutableClass = mutableClassDefBy(classDef)
                methods.map(mutableClass::findMutableMethodOf).forEach { method ->
                    patchedInstructions += method.injectLegacyPostIconResourceMapper(resourceIds)
                }
            }
        }

        if (patchedInstructions == 0) {
            throw PatchException("Could not find DCInside post icon resource references")
        }
    }
}

private fun drawableResourceIds(names: Collection<String>): Set<Int> =
    names.map { name ->
        getResourceId(ResourceType.DRAWABLE, name).toInt()
    }.toSet()

private fun Method.hasLegacyPostIconResourceLoad(resourceIds: Set<Int>): Boolean =
    implementation?.instructions?.any { instruction ->
        instruction.narrowLiteralRegister()?.second in resourceIds
    } == true

private fun MutableMethod.injectLegacyPostIconResourceMapper(resourceIds: Set<Int>): Int {
    val matches = instructions
        .mapIndexedNotNull { index, instruction ->
            instruction.narrowLiteralRegister()
                ?.takeIf { (_, literal) -> literal in resourceIds }
                ?.let { (register, _) -> index to register }
        }

    matches.asReversed().forEach { (index, register) ->
        addInstructions(
            index + 1,
            """
                invoke-static/range {v$register .. v$register}, $SETTINGS_CLASS->getLegacyPostIconResource(I)I
                move-result v$register
            """.trimIndent(),
        )
    }

    return matches.size
}

private fun Instruction.narrowLiteralRegister(): Pair<Int, Int>? {
    val register = (this as? OneRegisterInstruction)?.registerA ?: return null
    val literal = (this as? NarrowLiteralInstruction)?.narrowLiteral ?: return null
    return register to literal
}
