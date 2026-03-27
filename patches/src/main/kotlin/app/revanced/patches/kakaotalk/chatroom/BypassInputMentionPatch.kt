package app.revanced.patches.kakaotalk.chatroom

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatroom.fingerprints.mentionComponentIsMultiChatFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val bypassInputMentionPatch = bytecodePatch(
    name = "Bypass input mention limit in non-multichat",
    description = "Bypass the limit of input mentions in non-multichat rooms",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        mentionComponentIsMultiChatFingerprint.method.replaceInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )
    }
}