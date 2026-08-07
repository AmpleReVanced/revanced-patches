package app.revanced.patches.kakaotalk.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.integrity.fingerprints.AbuseDetectIntegrityTokenFingerprint
import app.revanced.patches.kakaotalk.integrity.fingerprints.AbuseDetectReportSenderFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableAbuseDetectionReportPatch = bytecodePatch(
    name = "Disable abuse detection report",
    description = "Stops the abuse detection report that attests the app to the server on startup and " +
            "during login, which cannot be spoofed locally because it carries a Google-signed Play " +
            "Integrity token that exposes the re-signed certificate.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        AbuseDetectReportSenderFingerprint.method.returnEarly()

        AbuseDetectIntegrityTokenFingerprint.method.addInstructions(
            0,
            """
                const-string v0, ""
                return-object v0
            """.trimIndent()
        )
    }
}
