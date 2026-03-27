package app.revanced.patches.kakaotalk.send

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.send.fingerprints.isEnableSendBigTextFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n

@Suppress("unused")
val enableSendBigTextPatch = bytecodePatch(
    name = "Enable send big text",
    description = "Allows sending big text messages in KakaoTalk.",
    default = false // Starting from newer version, the Quiet Send feature has been added, causing conflicts with the entry point for that feature. Therefore, it is disabled by default
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        isEnableSendBigTextFingerprint.method.instructions.indexOfFirst {
            it.opcode == Opcode.CONST_4 && (it as BuilderInstruction11n).narrowLiteral == 0x0
        }.takeIf { it >= 0 }
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