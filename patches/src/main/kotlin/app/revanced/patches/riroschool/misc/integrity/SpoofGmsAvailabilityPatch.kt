package app.revanced.patches.riroschool.misc.integrity

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.revanced.patches.riroschool.shared.requireRiroTarget
import app.revanced.patches.riroschool.shared.riroCompatibility

/** Opt-in app-local startup GMS availability spoofing, independent from diagnostics collection. Does not install services or modify actual module availability. */
val spoofGmsAvailabilityPatch = bytecodePatch(
    name = "Spoof GMS availability",
    description = "Treat the app startup Google Play Services availability check as SUCCESS. Does not install GMS or change analytics collection.",
    default = false,
) {
    compatibleWith(riroCompatibility)
    execute {
        requireRiroTarget(packageMetadata)
        spoofGmsAvailability()
    }
}
