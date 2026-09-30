package app.revanced.patches.starnote.premium

import app.morphe.patcher.patch.rawResourcePatch
import app.revanced.patches.starnote.shared.Constants.COMPATIBILITY_STARNOTE
import app.revanced.patches.starnote.shared.ProtectedDex
import app.revanced.patches.starnote.shared.loadProtectedDexPatch

private const val LOCAL_NOTE_QUOTA = 1_000_000L

internal val unlockPremiumStatePatch = rawResourcePatch {
    dependsOn(loadProtectedDexPatch)

    execute {
        val premiumClass = ProtectedDex.returnEarly(PermanentPremiumFingerprint, true).definingClass
        ProtectedDex.returnEarly(PremiumRoleFingerprint, true, premiumClass)
        ProtectedDex.returnLong(NoteCreateQuotaFingerprint, LOCAL_NOTE_QUOTA)
    }
}

@Suppress("unused")
val unlockPremiumFeaturesPatch = rawResourcePatch(
    name = "Unlock Premium features",
    description = "Enables local premium features and raises the local note creation quota.",
) {
    compatibleWith(COMPATIBILITY_STARNOTE)

    dependsOn(unlockPremiumStatePatch)
}