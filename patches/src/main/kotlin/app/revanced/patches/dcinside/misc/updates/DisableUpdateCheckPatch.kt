package app.revanced.patches.dcinside.misc.updates

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE

@Suppress("unused")
val disableUpdateCheckPatch = patch(
    name = "Disable update check",
    description = "Disables the app's update check.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        CheckIsRecentVersionFingerprint.method.returnEarly(false)
    }
}