package app.revanced.patches.soop.ads

import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.soop.ads.fingerprints.*
import app.revanced.patches.soop.common.fingerprints.KotlinUnitInstanceFingerprint
import app.revanced.patches.soop.common.utils.returnUnitEarly
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP

@Suppress("unused")
val removeLiveBannerAdsPatch = bytecodePatch(
    name = "Remove live banner ads",
    description = "Removes the banner ads shown under the live player by stopping their driver " +
        "from attaching the banner fragments.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        LiveBannerDriverFingerprint.method.returnUnitEarly(KotlinUnitInstanceFingerprint.originalClassDef)
    }
}