package app.revanced.patches.kakaotalk.integrity

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.integrity.fingerprints.utilityGetSignatureFingerprint

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Spoofs the app signature to pass integrity checks.",
) {
    compatibleWith("com.kakao.talk"("25.10.1"))

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