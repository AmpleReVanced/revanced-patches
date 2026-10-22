package app.revanced.patches.soop.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.soop.ads.fingerprints.AdBalloonBannerFingerprint
import app.revanced.patches.soop.ads.fingerprints.adBalloonBannerContainerFingerprint
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP

@Suppress("unused")
val removeAdBalloonBannerPatch = bytecodePatch(
    name = "Remove Ad Balloon banner",
    description = "Removes Ad Balloon banners displayed over the live player.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        adBalloonBannerContainerFingerprint().method.returnEarly()
        AdBalloonBannerFingerprint.method.returnEarly()
    }
}