package app.revanced.patches.dcinside.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.ads.fingerprints.HideMainAdFingerprint
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE

@Suppress("unused")
val hideMainAdPatch = bytecodePatch(
    name = "Hide Main Ad",
    description = "Hides the main ad in the app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        HideMainAdFingerprint.method.addInstructions(
            0,
            """
                const/16 v0, 0x8
                invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                
                const/4 v0, 0x0
                invoke-virtual {p1, v0}, Landroid/view/View;->setMinimumHeight(I)V
            """.trimIndent()
        )
    }
}