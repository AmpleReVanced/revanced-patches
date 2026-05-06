package app.revanced.patches.dcinside.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.dcinside.ads.fingerprints.DisableAdControllerFingerprint
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE

@Suppress("unused")
val disableAdControllerPatch = bytecodePatch(
    name = "Disable ad controller",
    description = "Disables the ad controller that manages ads in the app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        DisableAdControllerFingerprint.method.returnEarly()
    }
}