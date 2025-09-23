package app.revanced.patches.kakaotalk.integrity

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.common.fingerprints.kotlinUnitInstanceFingerprint
import app.revanced.patches.kakaotalk.integrity.fingerprints.requestChecksumsFingerprint

@Suppress("unused")
val bypassRequestChecksumPatch = bytecodePatch(
    name = "Bypass requestChecksums",
    description = "Prevents the execution of checksum verification logic by making it return early."
) {
    compatibleWith("com.kakao.talk"("25.8.0"))

    execute {
        val findUnit = kotlinUnitInstanceFingerprint.method
        val unitClass = findUnit.definingClass

        val method = requestChecksumsFingerprint.method

        method.addInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )
    }
}