package app.revanced.patches.kakaotalk.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.misc.fingerprints.ConfigConstructorFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val forceEnableDebugModePatch = bytecodePatch(
    name = "Force enable debug mode",
    description = "Enables debug mode in the app.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch)

    execute {
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
