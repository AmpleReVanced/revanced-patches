package app.revanced.patches.flexcil.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.flexcil.shared.Constants.COMPATIBILITY_FLEXCIL

@Suppress("unused")
val unlockPremiumFeaturesPatch = bytecodePatch(
    name = "Unlock Premium features",
    description = "Enables app features locked behind the subscription paywall.",
) {
    compatibleWith(COMPATIBILITY_FLEXCIL)

    execute {
        IsCanUsePremiumFeaturesFingerprint.method.returnEarly(true)
        IsPurchaseSpecificProductFingerprint.method.returnEarly(true)
        IsDownloadablePremiumContent.method.returnEarly(true)
    }
}