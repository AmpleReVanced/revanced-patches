package app.revanced.patches.soop.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.soop.ads.fingerprints.*
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP

@Suppress("unused")
val removeCatchAdsPatch = bytecodePatch(
    name = "Remove Catch ads",
    description = "Removes the ads inserted between clips in the Catch feed by reporting no ad slots.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        CatchAdListFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
    }
}