package app.revanced.patches.kakaotalk.misc

import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.misc.fingerprints.isRecordingPauseResumeEnabled
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n

@Suppress("unused")
val enableRecordingPauseResumePatch = bytecodePatch(
    name = "Enable recording pause/resume feature",
    description = "Enable recording pause/resume feature in KakaoTalk",
) {
    compatibleWith("com.kakao.talk"("25.8.3"))

    execute {
        isRecordingPauseResumeEnabled.method.instructions.indexOfFirst { it.opcode == Opcode.CONST_4 && (it as BuilderInstruction11n).narrowLiteral == 0x0 }
            .takeIf { it >= 0 }
            ?.let { index ->
                isRecordingPauseResumeEnabled.method.replaceInstruction(
                    index,
                    BuilderInstruction11n(
                        Opcode.CONST_4,
                        (isRecordingPauseResumeEnabled.method.getInstruction(index) as BuilderInstruction11n).registerA,
                        0x1
                    )
                )
            }
    }
}