package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ads.fingerprints.chatListAdHelperEnabledFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableChatRoomListAdPatch = bytecodePatch(
    name = "Disable chat room list ad",
    description = "Disable the chat room list ad.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        chatListAdHelperEnabledFingerprint.method.returnEarly(false)
    }
}