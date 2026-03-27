package app.revanced.patches.kakaotalk.tracker

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tracker.fingerprints.DisableSentryFingerprint
import app.revanced.patches.shared.misc.privacy.disableSentryTelemetryPatch

@Suppress("unused")
val disableSentryPatch = bytecodePatch(
    name = "Disable Sentry",
    description = "Disables Sentry error reporting in KakaoTalk."
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(disableSentryTelemetryPatch)
    execute {
        DisableSentryFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )
    }
}