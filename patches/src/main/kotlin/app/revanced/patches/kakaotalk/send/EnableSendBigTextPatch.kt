package app.revanced.patches.kakaotalk.send

import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.send.fingerprints.isEnableSendBigTextFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n

@Suppress("unused")
val enableSendBigTextPatch = bytecodePatch(
    name = "Enable send big text",
    description = "Allows sending big text messages in KakaoTalk.",
) {
    compatibleWith("com.kakao.talk"("25.8.3"))

    execute {
        isEnableSendBigTextFingerprint.method.instructions.indexOfFirst { it.opcode == Opcode.CONST_4 && (it as BuilderInstruction11n).narrowLiteral == 0x0 }
            .takeIf { it >= 0 }
            ?.let { index ->
                isEnableSendBigTextFingerprint.method.replaceInstruction(
                    index,
                    BuilderInstruction11n(
                        Opcode.CONST_4,
                        (isEnableSendBigTextFingerprint.method.getInstruction(index) as BuilderInstruction11n).registerA,
                        0x1
                    )
                )
            }
    }
}