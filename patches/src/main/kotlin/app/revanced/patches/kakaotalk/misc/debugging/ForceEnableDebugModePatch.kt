package app.revanced.patches.kakaotalk.misc.debugging

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.debugging.fingerprints.ConfigConstructorFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

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
        val idxReturn = method.indexOfFirstInstructionOrThrow(Opcode.RETURN_VOID)
        val debugField = method.instructions.firstNotNullOfOrNull { instruction ->
            instruction.takeIf { it.opcode == Opcode.SPUT_BOOLEAN }
                ?.getReference<FieldReference>()
        } ?: throw PatchException("Could not find debug mode field")
        val register = method.getFreeRegisterProvider(idxReturn, 1).getFreeRegister()

        method.addInstructions(
            idxReturn,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->forceDebugMode()Z
                move-result v$register
                sput-boolean v$register, ${debugField.smaliReference}
            """.trimIndent()
        )
    }
}
