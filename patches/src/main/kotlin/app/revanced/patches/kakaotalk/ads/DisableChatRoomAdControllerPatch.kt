package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ads.fingerprints.OpenLinkChatAdControllerLoadFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableChatRoomAdControllerPatch = bytecodePatch(
    name = "Disable ChatRoomAdController",
    description = "Disables the open-link chat room BizBoard ad controller.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        OpenLinkChatAdControllerLoadFingerprint.method.returnEarly()
    }
}