package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.kakaotalk.ad.fingerprints.OpenLinkChatAdControllerLoadFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableChatRoomAdControllerPatch = patch(
    name = "Disable ChatRoomAdController",
    description = "Disables the open-link chat room BizBoard ad controller.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        OpenLinkChatAdControllerLoadFingerprint.method.returnEarly()
    }
}