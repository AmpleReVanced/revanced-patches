package app.revanced.patches.kakaotalk.misc.openlink

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getMutableMethod
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.openlink.fingerprints.BrowserIntentFingerprint
import app.revanced.patches.kakaotalk.misc.openlink.fingerprints.DefaultExternalBrowserFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/DefaultExternalBrowserPatch;"

private const val USE_EXTERNAL_BROWSER =
    "Lapp/revanced/extension/kakaotalk/settings/Settings;->useDefaultExternalBrowser()Z"

@Suppress("unused")
val defaultExternalBrowserPatch = patch(
    name = "Default external browser",
    description = "Sets the default external browser for KakaoTalk to the system's default browser.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            SwitchPreference(
                key = "morphe_pref_default_external_browser",
                titleKey = "morphe_settings_patch_external_browser",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        DefaultExternalBrowserFingerprint.apply {
            val index = instructionMatches.last().index
            val register = method.getInstruction<OneRegisterInstruction>(index).registerA
            method.replaceInstruction(index, "invoke-static {}, $USE_EXTERNAL_BROWSER")
            method.addInstruction(index + 1, "move-result v$register")
        }

        BrowserIntentFingerprint.instructionMatches.first().instruction
            .getReference<MethodReference>()!!
            .getMutableMethod()
            .addInstructions(
                0,
                """
                    invoke-static {}, $USE_EXTERNAL_BROWSER
                    move-result v0
                    return v0
                """,
            )
    }
}
