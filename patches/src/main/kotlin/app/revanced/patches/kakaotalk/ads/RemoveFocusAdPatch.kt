package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ads.fingerprints.LoadFocusAdFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val removeFocusAdPatch = bytecodePatch(
    name = "Remove focus ad",
    description = "Removes the focus ad from the app.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        LoadFocusAdFingerprint.method.returnEarly(true)
    }
}