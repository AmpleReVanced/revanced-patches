package app.revanced.patches.kakaotalk.chatroom

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatroom.fingerprints.IsPlayYoutubePlayerInChatRoomFingerprint
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.send.fingerprints.IsEnableSendBigTextFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n

@Suppress("unused")
val playYoutubePlayerInChatRoomPatch = bytecodePatch(
    name = "Play YouTube player in chat room",
    description = "Allows playing YouTube videos in KakaoTalk chat rooms.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch)

    execute {
        val method = IsPlayYoutubePlayerInChatRoomFingerprint.method
        val index = method.instructions.indexOfFirst {
            it.opcode == Opcode.CONST_4 && (it as BuilderInstruction11n).narrowLiteral in listOf(0x0, 0x1)
        }

        if (index < 0) {
            throw PatchException("Could not find const/4 default value in youtubePlayerInChatRoom")
        }

        val register = (method.getInstruction(index) as BuilderInstruction11n).registerA

        method.removeInstructions(index, 1)
        method.addInstructions(
            index,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->playYoutubePlayerInChatRoom()Z
                move-result v$register
            """.trimIndent()
        )
    }
}