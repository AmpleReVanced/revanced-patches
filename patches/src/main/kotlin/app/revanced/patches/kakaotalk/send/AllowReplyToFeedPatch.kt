package app.revanced.patches.kakaotalk.send

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.send.fingerprints.allowSwipeReplyToFeedFingerprint
import app.revanced.patches.kakaotalk.send.fingerprints.realActionForReplyFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21t

@Suppress("unused")
val allowReplyToFeedPatch = bytecodePatch(
    name = "Allow reply to feed",
    description = "Allows replying to feed messages",
) {
    compatibleWith("com.kakao.talk"("25.7.3"))

    execute {
        val patch: (Fingerprint) -> Unit = { fp ->
            val method = fp.method
            val insns = method.instructions

            val idxIfnez = insns.indexOfFirst { it is Instruction21t && it.opcode == Opcode.IF_NEZ }
            val idxIfnezTarget = (insns[idxIfnez] as Instruction21t).registerA

            val idxInvoke = insns.subList(0, idxIfnez)
                .indexOfLast { it.opcode == Opcode.INVOKE_VIRTUAL }

            (idxInvoke until idxIfnez).toList()
                .sortedDescending()
                .forEach { method.removeInstruction(it) }

            method.replaceInstruction(
                idxInvoke,
                BuilderInstruction11n(
                    Opcode.CONST_4,
                    idxIfnezTarget,
                    0x0,
                )
            )
        }

        patch(realActionForReplyFingerprint)
        patch(allowSwipeReplyToFeedFingerprint)
    }
}