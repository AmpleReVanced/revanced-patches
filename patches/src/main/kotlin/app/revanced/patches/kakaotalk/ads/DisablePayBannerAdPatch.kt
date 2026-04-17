package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ads.fingerprints.PayBannerLoadFingerprint
import app.revanced.patches.kakaotalk.ads.fingerprints.PayBannerRenderFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disablePayBannerAdPatch = bytecodePatch(
    name = "Disable Pay banner ad",
    description = "Disables the KakaoPay banner ad load and render paths.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        PayBannerLoadFingerprint.method.returnEarly()
        PayBannerRenderFingerprint.method.returnEarly()
    }
}