package app.revanced.patches.kakaotalk.ghost

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.common.fingerprints.kotlinUnitInstanceFingerprint
import app.revanced.patches.kakaotalk.ghost.fingerprints.sendCurrentActionFingerprint

@Suppress("unused")
val ghostMode = bytecodePatch(
    name = "Ghost Mode",
    description = "Don't expose your typing status to the other party.",
) {
    compatibleWith("com.kakao.talk"("25.8.0"))

    execute {
        val findUnit = kotlinUnitInstanceFingerprint.method
        val unitClass = findUnit.definingClass

        val method = sendCurrentActionFingerprint.method

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