package app.revanced.patches.dcinside.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.ads.fingerprints.postReadImageAdViewFingerprint
import app.revanced.patches.dcinside.ads.fingerprints.refreshImageAdFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val disableImageAdPatch = bytecodePatch(
    name = "Disable Image Ad",
    description = "Disables the image ad in the app.",
) {
    compatibleWith("com.dcinside.app.android"("5.2.5"))

    execute {
        postReadImageAdViewFingerprint.method.apply {
            val setGravityIndex = implementation!!.instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_VIRTUAL &&
                        (it as? ReferenceInstruction)?.getReference<MethodReference>()?.name == "setGravity"
            }

            addInstructions(
                setGravityIndex + 1,
                """
                    const/4 v0, 0x2
                    iput v0, p0, Lcom/dcinside/app/view/PostReadImageAdView;->a:I
                    
                    const/4 p1, 0x0
                    invoke-direct {p0, p1}, Lcom/dcinside/app/view/PostReadImageAdView;->setAdViewHeight(I)V
                    
                    const/16 p1, 0x8
                    invoke-virtual {p0, p1}, Landroid/view/View;->setVisibility(I)V
                    
                    return-void
                """.trimIndent()
            )
        }

        refreshImageAdFingerprint.method.addInstruction(
            0,
            "return-void"
        )
    }
}