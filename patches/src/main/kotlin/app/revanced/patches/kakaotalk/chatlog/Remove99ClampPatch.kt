package app.revanced.patches.kakaotalk.chatlog

import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatlog.fingerprints.getWatermarkCountFromCacheFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.processWatermarkCountFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22t

@Suppress("unused")
val remove99ClampPatch = bytecodePatch(
    name = "Disable 99 unread limit",
    description = "Skip the 99-cap so unread count shows full value"
) {
    compatibleWith("com.kakao.talk"("25.11.1"))

    execute {
        val processWatermarkCountMethod = processWatermarkCountFingerprint.method

        processWatermarkCountMethod.apply {
            replaceInstruction(instructions.size - 2, "nop")
        }

        val getWatermarkCountFromCacheMethod = getWatermarkCountFromCacheFingerprint.method

        getWatermarkCountFromCacheMethod.instructions
            .filterIsInstance<BuilderInstruction22t>()
            .filter { it.opcode == Opcode.IF_LE }
            .forEach { ifle ->
                val idx = getWatermarkCountFromCacheMethod.instructions.indexOf(ifle)
                val goto = BuilderInstruction10t(Opcode.GOTO, ifle.target)
                getWatermarkCountFromCacheMethod.replaceInstruction(idx, goto)
            }
    }
}
