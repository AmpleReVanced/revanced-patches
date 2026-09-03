package app.revanced.patches.starnote.premium

import app.morphe.patcher.patch.rawResourcePatch
import app.revanced.patches.starnote.shared.Constants.COMPATIBILITY_STARNOTE
import app.revanced.patches.starnote.shared.ProtectedDex
import app.revanced.patches.starnote.shared.loadProtectedDexPatch

internal val unlockPremiumStatePatch = rawResourcePatch {
    dependsOn(loadProtectedDexPatch)

    execute {
        val premiumClass = ProtectedDex.returnEarly(PermanentPremiumFingerprint, true).definingClass
        ProtectedDex.returnEarly(PremiumRoleFingerprint, true, premiumClass)
    }
}

@Suppress("unused")
val unlockPremiumFeaturesPatch = rawResourcePatch(
    name = "Unlock Premium features",
    description = "Enables app features locked behind the subscription paywall.",
) {
    compatibleWith(COMPATIBILITY_STARNOTE)

    dependsOn(unlockPremiumStatePatch)
}