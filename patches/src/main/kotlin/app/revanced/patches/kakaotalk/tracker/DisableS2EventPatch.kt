package app.revanced.patches.kakaotalk.tracker

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.common.fingerprints.kotlinUnitInstanceFingerprint
import app.revanced.patches.kakaotalk.tracker.fingerprints.disableSaveS2EventFingerprint
import app.revanced.patches.kakaotalk.tracker.fingerprints.sendS2EventFingerprint

@Suppress("unused")
val disableS2EventPatch = bytecodePatch(
    name = "Disable S2Event",
    description = "Disables Tracker",
) {
    compatibleWith("com.kakao.talk"("25.11.0"))

    execute {
        disableSaveS2EventFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0         
                return-object v0
            """.trimIndent()
        )

        val findUnit = kotlinUnitInstanceFingerprint.method
        val unitClass = findUnit.definingClass
        // I tried to find the field name, but it's pretty obvious to me, so I hardcode it.
        // If it changes, we need to fix it
        sendS2EventFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $unitClass->a:$unitClass
                return-object v0
            """.trimIndent()
        )
    }
}