package app.revanced.patches.dcinside.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.revanced.patches.dcinside.ad.fingerprints.HideMainAdFingerprint
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val hideMainAdPatch = patch(
    name = "Hide Main Ad",
    description = "Hides the main ad in the app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        val fingerprint = HideMainAdFingerprint
        val method = fingerprint.method
        val superCall = method.instructions.indexOfFirst {
            it.getReference<MethodReference>()?.let { call ->
                call.name == "<init>" && call.definingClass == fingerprint.classDef.superclass
            } == true
        }
        check(superCall >= 0) { "Could not find the main ad holder superclass constructor" }
        method.addInstructions(
            superCall + 1,
            """
                const/16 v0, 0x8
                invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                
                const/4 v0, 0x0
                invoke-virtual {p1, v0}, Landroid/view/View;->setMinimumHeight(I)V
                return-void
            """.trimIndent()
        )
    }
}