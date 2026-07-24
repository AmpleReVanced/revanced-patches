package app.revanced.patches.soop.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.soop.ads.fingerprints.*
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP

@Suppress("unused")
val removeIntroAdsPatch = bytecodePatch(
    name = "Remove intro ads",
    description = "Removes the image, video and cinema intro ad banners shown on launch.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        IntroAdBannerFingerprint.method.returnEarly()
    }
}