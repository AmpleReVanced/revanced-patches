package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ad.fingerprints.CecallFolderBizBoardEnabledFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.ChatListAdHelperEnabledFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.ChatListBizBoardEnabledFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.ChatListGlobalAdEnabledFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableChatRoomListAdPatch = bytecodePatch(
    name = "Disable chat room list ad",
    description = "Disables ads in the chat room list and Call filter.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        ChatListBizBoardEnabledFingerprint.method.returnEarly(false)
        ChatListAdHelperEnabledFingerprint.method.returnEarly(false)
        ChatListGlobalAdEnabledFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """.trimIndent()
        )
        CecallFolderBizBoardEnabledFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """.trimIndent()
        )
    }
}
