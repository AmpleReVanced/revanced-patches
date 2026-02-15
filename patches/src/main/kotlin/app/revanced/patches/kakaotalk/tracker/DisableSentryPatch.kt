package app.revanced.patches.kakaotalk.tracker

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.tracker.fingerprints.disableSentryFingerprint
import app.revanced.patches.shared.misc.privacy.disableSentryTelemetryPatch

@Suppress("unused")
val disableSentryPatch = bytecodePatch(
    name = "Disable Sentry",
    description = "Disables Sentry error reporting in KakaoTalk."
) {
    compatibleWith("com.kakao.talk"("26.1.3"))
    dependsOn(disableSentryTelemetryPatch)
    execute {
        disableSentryFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )
    }
}