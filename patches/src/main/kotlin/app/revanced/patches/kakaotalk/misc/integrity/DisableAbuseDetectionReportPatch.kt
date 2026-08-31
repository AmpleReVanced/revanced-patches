package app.revanced.patches.kakaotalk.misc.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.AbuseDetectIntegrityTokenFingerprint
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
        val method = AbuseDetectIntegrityTokenFingerprint.method
        val registers = method.getFreeRegisterProvider(0, 3)
        val pairRegister = registers.getFreeRegister4Bit()
        val tokenRegister = registers.getFreeRegister4Bit()
        val errorRegister = registers.getFreeRegister4Bit()

        method.addInstructions(
            0,
            """
                new-instance v$pairRegister, Lkotlin/Pair;
                const-string v$tokenRegister, ""
                const/4 v$errorRegister, -0x3
                invoke-static {v$errorRegister}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                move-result-object v$errorRegister
                invoke-direct {v$pairRegister, v$tokenRegister, v$errorRegister}, Lkotlin/Pair;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                return-object v$pairRegister
            """.trimIndent(),
        )
    }
}
