package app.revanced.patches.kakaotalk.misc

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.misc.fingerprints.configConstructorFingerprint
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val forceEnableDebugModePatch = bytecodePatch(
    name = "Force enable debug mode",
    description = "Enables debug mode in the app.",
) {
    compatibleWith("com.kakao.talk"("26.1.2"))

    execute {
        val method = configConstructorFingerprint.method
        val insns = method.instructions
        val idxReturn = insns.indexOfFirst { it.opcode == Opcode.RETURN_VOID } // RETURN_VOID

        val clazz = method.definingClass

        method.addInstructions(
            idxReturn,
            """
                const/4 v0, 0x1
                sput-boolean v0, $clazz->b:Z
            """.trimIndent()
        )
    }
}