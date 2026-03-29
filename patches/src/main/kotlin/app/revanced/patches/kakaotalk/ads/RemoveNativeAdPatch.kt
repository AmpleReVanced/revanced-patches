package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ads.fingerprints.LoadNativeAdFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val removeNativeAdPatch = bytecodePatch(
    name = "Remove native ad",
    description = "Removes the native ad from the app.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        LoadNativeAdFingerprint.method.returnEarly(true)
    }
}