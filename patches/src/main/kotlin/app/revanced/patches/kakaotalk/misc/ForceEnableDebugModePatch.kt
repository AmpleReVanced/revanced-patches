package app.revanced.patches.kakaotalk.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.fingerprints.ConfigConstructorFingerprint
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.Opcode

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/ForceEnableDebugModePatch;"

@Suppress("unused")
val forceEnableDebugModePatch = bytecodePatch(
    name = "Force enable debug mode",
    description = "Enables debug mode in the app.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.DEBUG.addPreferences(
            SwitchPreference(
                key = "morphe_pref_force_debug_mode",
                titleKey = "morphe_settings_patch_force_debug_mode",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val method = ConfigConstructorFingerprint.method
        val insns = method.instructions
        val idxReturn = insns.indexOfFirst { it.opcode == Opcode.RETURN_VOID } // RETURN_VOID

        val clazz = method.definingClass

        method.addInstructions(
            idxReturn,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->forceDebugMode()Z
                move-result v0
                sput-boolean v0, $clazz->b:Z
            """.trimIndent()
        )
    }
}
