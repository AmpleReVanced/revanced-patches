package app.revanced.patches.kakaotalk.misc.tracker

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.DisableSentryFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.shared.misc.privacy.disableSentryTelemetryPatch

@Suppress("unused")
val disableSentryPatch = patch(
    name = "Disable Sentry",
    description = "Disables Sentry error reporting in KakaoTalk."
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(disableSentryTelemetryPatch)
    execute {
        DisableSentryFingerprint.method.returnEarly()
    }
}