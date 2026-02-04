package app.revanced.patches.dcinside.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.ads.fingerprints.hideMainAdFingerprint

@Suppress("unused")
val hideMainAdPatch = bytecodePatch(
    name = "Hide Main Ad",
    description = "Hides the main ad in the app.",
) {
    compatibleWith("com.dcinside.app.android"("5.2.6"))

    execute {
        val method = hideMainAdFingerprint.method

        method.addInstructions(
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