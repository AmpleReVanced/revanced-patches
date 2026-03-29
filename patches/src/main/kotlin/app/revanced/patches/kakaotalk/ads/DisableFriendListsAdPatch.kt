package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ads.fingerprints.CheckDisableFriendListsAdFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableFriendListsAdPatch = bytecodePatch(
    name = "Disable Friend Lists ad",
    description = "Disables the Friend Lists ad in KakaoTalk.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        CheckDisableFriendListsAdFingerprint.method.returnEarly(true)
    }
}