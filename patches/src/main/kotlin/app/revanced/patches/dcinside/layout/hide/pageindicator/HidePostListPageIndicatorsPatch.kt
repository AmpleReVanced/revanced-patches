package app.revanced.patches.dcinside.layout.hide.pageindicator

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/HidePostListPageIndicatorsPatch;"

@Suppress("unused")
val hidePostListPageIndicatorsPatch = bytecodePatch(
    name = "Hide post list page indicators",
    description = "Adds a setting to hide the Page N indicators between pages in post lists.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addSettingsPatch, resourceMappingPatch)

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            SwitchPreference(
                key = "morphe_pref_hide_post_list_page_indicators",
                titleKey = "morphe_settings_hide_post_list_page_indicators",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        PostListPageIndicatorFingerprint.run {
            method.injectVisibilityOverride(instructionMatches.single().index)
        }
        SearchResultPageIndicatorFingerprint.run {
            method.injectVisibilityOverride(instructionMatches.single().index)
        }
    }
}

private fun MutableMethod.injectVisibilityOverride(resourceIndex: Int) {
    val visibilityIndex = indexOfFirstInstructionReversedOrThrow(resourceIndex) {
        opcode == Opcode.INVOKE_VIRTUAL &&
            getReference<MethodReference>()?.let { reference ->
                reference.definingClass == "Landroid/view/View;" &&
                    reference.name == "setVisibility" &&
                    reference.parameterTypes == listOf("I") &&
                    reference.returnType == "V"
            } == true
    }
    val instruction = getInstruction<FiveRegisterInstruction>(visibilityIndex)

    replaceInstruction(
        visibilityIndex,
        "invoke-static {v${instruction.registerC}, v${instruction.registerD}}, " +
            "$EXTENSION_CLASS->setPageIndicatorVisibility(Landroid/view/View;I)V",
    )
}