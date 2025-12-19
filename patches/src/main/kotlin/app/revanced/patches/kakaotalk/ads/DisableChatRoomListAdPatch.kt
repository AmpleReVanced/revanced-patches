package app.revanced.patches.kakaotalk.ads

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ads.fingerprints.chatListAdHelperEnabledFingerprint

@Suppress("unused")
val disableChatRoomListAdPatch = bytecodePatch(
    name = "Disable chat room list ad",
    description = "Disable the chat room list ad.",
) {
    compatibleWith("com.kakao.talk"("25.11.0"))

    execute {
        val chatListAdHelperEnabledMethod = chatListAdHelperEnabledFingerprint.method
        chatListAdHelperEnabledMethod.replaceInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent()
        )
    }
}