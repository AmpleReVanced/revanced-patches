package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ad.fingerprints.ChatListAdHelperEnabledFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableChatRoomListAdPatch = bytecodePatch(
    name = "Disable chat room list ad",
    description = "Disable the chat room list ad.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        ChatListAdHelperEnabledFingerprint.method.returnEarly(false)
    }
}