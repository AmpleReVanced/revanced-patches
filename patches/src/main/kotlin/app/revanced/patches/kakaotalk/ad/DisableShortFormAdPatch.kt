package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ad.fingerprints.ShortFormAdLoadFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.ShortFormAdRenderFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableShortFormAdPatch = bytecodePatch(
    name = "Disable ShortForm ad",
    description = "Disables the shortform ad load and render paths.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        ShortFormAdLoadFingerprint.method.returnEarly()
        ShortFormAdRenderFingerprint.method.returnEarly()
    }
}