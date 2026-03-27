package app.revanced.patches.kakaotalk.integrity

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.integrity.fingerprints.utilityGetSignatureFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Spoofs the app signature to pass integrity checks.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val method = utilityGetSignatureFingerprint.method
        method.replaceInstructions(
            0,
            """
                const-string v0, "7MRbkCrB6DyL4XWKJX5nSS3jdFY="
                return-object v0
            """.trimIndent()
        )
    }
}