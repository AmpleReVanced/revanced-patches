package app.revanced.patches.soop.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.soop.ads.fingerprints.*
import app.revanced.patches.soop.common.fingerprints.KotlinUnitInstanceFingerprint
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP

@Suppress("unused")
val removeListBannerAdsPatch = bytecodePatch(
    name = "Remove list banner ads",
    description = "Removes the native banner ads under the live player and in feeds by stopping their " +
        "ad request, so no banner is fetched or shown.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        val unitClass = KotlinUnitInstanceFingerprint.classDef.type
        ListBannerAdRequestFingerprint.matchAll().forEach {
            it.method.addInstructions(
                0,
                """
                    sget-object v0, $unitClass->a:$unitClass
                    return-object v0
                """.trimIndent()
            )
        }
        LiveDownBannerRequestFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $unitClass->a:$unitClass
                return-object v0
            """.trimIndent()
        )
    }
}