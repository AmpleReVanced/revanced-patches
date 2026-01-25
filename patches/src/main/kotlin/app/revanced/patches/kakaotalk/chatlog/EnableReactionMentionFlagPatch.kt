package app.revanced.patches.kakaotalk.chatlog

import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatlog.fingerprints.reactionMentionFlagFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n

@Suppress("unused")
val enableReactionMentionFlagPatch = bytecodePatch(
    name = "Enable reaction mention flag",
    description = "Enables the reaction mention flag in KakaoTalk chat logs.",
    use = false // This patch is currently disabled due to issues with the KakaoTalk app.
) {
    compatibleWith("com.kakao.talk"("26.1.1"))

    execute {
        reactionMentionFlagFingerprint.method.instructions.indexOfFirst { it.opcode == Opcode.CONST_4 && (it as BuilderInstruction11n).narrowLiteral == 0x0 }
            .takeIf { it >= 0 }
            ?.let { index ->
                reactionMentionFlagFingerprint.method.replaceInstruction(
                    index,
                    BuilderInstruction11n(
                        Opcode.CONST_4,
                        (reactionMentionFlagFingerprint.method.getInstruction(index) as BuilderInstruction11n).registerA,
                        0x1
                    )
                )
            }
    }
}