package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ads.fingerprints.chatRoomAdViewControllerEnabledFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableChatRoomAdControllerPatch = bytecodePatch(
    name = "Disable ChatRoomAdController",
    description = "Disables the ChatRoomAdController to prevent ads from being shown in chat room list",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

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