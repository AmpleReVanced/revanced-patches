package app.revanced.patches.kakaotalk.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.integrity.fingerprints.intentResolveClientMethod
import app.revanced.patches.kakaotalk.integrity.fingerprints.verifyingSignatureFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val verifyingSignaturePatch = bytecodePatch(
    name = "Disable verifying signature",
    description = "Disables the signature verification check that prevents the app from running.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

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