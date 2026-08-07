package app.revanced.patches.kakaotalk.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.integrity.fingerprints.AbuseDetectIntegrityTokenFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableAbuseDetectionReportPatch = bytecodePatch(
    name = "Disable abuse detection report",
    description = "Answers the startup and login integrity challenge with an empty token and a benign " +
            "Play Integrity failure code instead of a Google-signed token that would expose the " +
            "re-signed certificate. The report itself is still sent, so the client does not go " +
            "conspicuously silent on the server's nonce the way suppressing it entirely would.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        // The report sender is left running on purpose: a stock client always answers the server's
        // nonce, even when Play Integrity fails, so total silence is itself a stock-impossible signal.
        // Only the token requester is stubbed, returning the same (emptyToken, errorCode) Pair the
        // app's own failure branch produces. -3 is Play Integrity NETWORK_ERROR, a transient failure
        // that implies nothing about the device.
        AbuseDetectIntegrityTokenFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Lkotlin/Pair;
                const-string v1, ""
                const/4 v2, -0x3
                invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                move-result-object v2
                invoke-direct {v0, v1, v2}, Lkotlin/Pair;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                return-object v0
            """.trimIndent(),
        )
    }
}
