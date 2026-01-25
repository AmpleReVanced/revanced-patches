package app.revanced.patches.kakaotalk.integrity

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.integrity.fingerprints.intentResolveClientMethod
import app.revanced.patches.kakaotalk.integrity.fingerprints.verifyingSignatureFingerprint

@Suppress("unused")
val verifyingSignaturePatch = bytecodePatch(
    name = "Disable verifying signature",
    description = "Disables the signature verification check that prevents the app from running.",
) {
    compatibleWith("com.kakao.talk"("26.1.1"))

    execute {
        verifyingSignatureFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )

        intentResolveClientMethod.method.replaceInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )
    }
}