package app.revanced.patches.soop.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.soop.ads.fingerprints.*
import app.revanced.patches.soop.common.fingerprints.KotlinUnitInstanceFingerprint
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP

@Suppress("unused")
val removeLiveBannerAdsPatch = bytecodePatch(
    name = "Remove live banner ads",
    description = "Removes the banner ads shown under the live player by stopping their driver " +
        "from attaching the banner fragments.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        val unitClass = KotlinUnitInstanceFingerprint.classDef.type
        LiveBannerDriverFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $unitClass->a:$unitClass
                return-object v0
            """.trimIndent()
        )
    }
}