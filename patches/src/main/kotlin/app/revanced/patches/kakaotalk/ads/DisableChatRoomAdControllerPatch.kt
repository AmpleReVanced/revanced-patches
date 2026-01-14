package app.revanced.patches.kakaotalk.ads

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ads.fingerprints.chatRoomAdViewControllerEnabledFingerprint

@Suppress("unused")
val disableChatRoomAdControllerPatch = bytecodePatch(
    name = "Disable ChatRoomAdController",
    description = "Disables the ChatRoomAdController to prevent ads from being shown in chat room list",
) {
    compatibleWith("com.kakao.talk"("25.11.2"))

    execute {
        val chatRoomAdViewControllerEnabledMethod = chatRoomAdViewControllerEnabledFingerprint.method
        chatRoomAdViewControllerEnabledMethod.replaceInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent()
        )
    }
}