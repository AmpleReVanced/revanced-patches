package app.revanced.patches.kakaotalk.misc.openlink

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.openlink.fingerprints.DefaultExternalBrowserFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/DefaultExternalBrowserPatch;"

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
            method.replaceInstruction(
                index,
                "invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->useDefaultExternalBrowser()Z",
            )
            method.addInstruction(index + 1, "move-result v$register")
        }
    }
}
