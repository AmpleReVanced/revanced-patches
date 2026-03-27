package app.revanced.patches.kakaotalk.chatroom

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatroom.fingerprints.isPlayYoutubePlayerInChatRoomFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n

@Suppress("unused")
val playYoutubePlayerInChatRoomPatch = bytecodePatch(
    name = "Play YouTube player in chat room",
    description = "Allows playing YouTube videos in KakaoTalk chat rooms.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        isPlayYoutubePlayerInChatRoomFingerprint.method.instructions.indexOfFirst { it.opcode == Opcode.CONST_4 && (it as BuilderInstruction11n).narrowLiteral == 0x0 }
            .takeIf { it >= 0 }
            ?.let { index ->
                isPlayYoutubePlayerInChatRoomFingerprint.method.replaceInstruction(
                    index,
                    BuilderInstruction11n(
                        Opcode.CONST_4,
                        (isPlayYoutubePlayerInChatRoomFingerprint.method.getInstruction(index) as BuilderInstruction11n).registerA,
                        0x1
                    )
                )
            }
    }
}