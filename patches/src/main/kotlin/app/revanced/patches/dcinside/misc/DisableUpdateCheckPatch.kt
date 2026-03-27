package app.revanced.patches.dcinside.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE

@Suppress("unused")
val disableUpdateCheckPatch = bytecodePatch(
    name = "Disable update check",
    description = "Disables the app's update check.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        DisableUpdateCheckFingerprint.method.returnEarly(false)
    }
}