package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ad.fingerprints.PayBannerLoadFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.PayBannerRenderFingerprint
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