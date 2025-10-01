package app.revanced.patches.kakaotalk.chatroom

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatroom.fingerprints.mentionComponentIsMultiChatFingerprint

@Suppress("unused")
val bypassInputMentionPatch = bytecodePatch(
    name = "Bypass input mention limit in non-multichat",
    description = "Bypass the limit of input mentions in non-multichat rooms",
) {
    compatibleWith("com.kakao.talk"("25.8.2"))

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