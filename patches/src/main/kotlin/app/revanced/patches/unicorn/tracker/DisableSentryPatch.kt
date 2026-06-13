package app.revanced.patches.unicorn.tracker

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.shared.misc.privacy.disableSentryTelemetryPatch
import app.revanced.patches.unicorn.shared.Constants.COMPATIBILITY_UNICORN

@Suppress("unused")
val disableSentryPatch = bytecodePatch(
    name = "Disable Sentry",
    description = "Disables Sentry reporting"
) {
    compatibleWith(COMPATIBILITY_UNICORN)
    dependsOn(disableSentryTelemetryPatch)
    execute {
        DisableSentryFingerprint.method.returnEarly()
    }
}