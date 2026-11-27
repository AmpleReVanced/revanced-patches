package app.revanced.patches.soop.ads

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.soop.ads.fingerprints.*
import app.revanced.patches.soop.common.fingerprints.KotlinUnitInstanceFingerprint
import app.revanced.patches.soop.common.utils.returnUnitEarly
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP

@Suppress("unused")
val removeListBannerAdsPatch = patch(
    name = "Remove list banner ads",
    description = "Removes the native banner ads under the live player and in feeds by stopping their " +
        "ad request, so no banner is fetched or shown.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        val unitClass = KotlinUnitInstanceFingerprint.originalClassDef

        ListBannerAdRequestFingerprint.method.returnEarly(null)
        LiveDownBannerRequestFingerprint.method.returnUnitEarly(unitClass)
    }
}