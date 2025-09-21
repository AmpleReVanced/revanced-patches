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
    compatibleWith("com.kakao.talk"("25.7.3"))

    execute {
        val findUnit = kotlinUnitInstanceFingerprint.method
        val unitClass = findUnit.definingClass

        val method = requestChecksumsFingerprint.method

        // I tried to find the field name, but it's pretty obvious to me, so I hardcode it.
        // If it changes, we need to fix it
        method.addInstructions(
            0,
            """
                sget-object v0, $unitClass->a:$unitClass
                return-object v0
            """.trimIndent()
        )
    }
}