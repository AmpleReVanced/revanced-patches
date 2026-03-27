package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ads.fingerprints.ChatRoomAdViewControllerEnabledFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableChatRoomAdControllerPatch = bytecodePatch(
    name = "Disable ChatRoomAdController",
    description = "Disables the ChatRoomAdController to prevent ads from being shown in chat room list",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        ChatRoomAdViewControllerEnabledFingerprint.method.returnEarly(false)
    }
}