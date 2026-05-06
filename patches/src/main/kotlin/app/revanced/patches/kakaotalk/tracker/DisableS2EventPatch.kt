package app.revanced.patches.kakaotalk.tracker

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.common.fingerprints.KotlinUnitInstanceFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tracker.fingerprints.DisableSaveS2EventFingerprint
import app.revanced.patches.kakaotalk.tracker.fingerprints.SendS2EventFingerprint

@Suppress("unused")
val disableS2EventPatch = bytecodePatch(
    name = "Disable S2Event",
    description = "Disables Tracker",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        DisableSaveS2EventFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0         
                return-object v0
            """.trimIndent()
        )

        val findUnit = KotlinUnitInstanceFingerprint.method
        val unitClass = findUnit.definingClass
        // I tried to find the field name, but it's pretty obvious to me, so I hardcode it.
        // If it changes, we need to fix it
        SendS2EventFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $unitClass->a:$unitClass
                return-object v0
            """.trimIndent()
        )
    }
}