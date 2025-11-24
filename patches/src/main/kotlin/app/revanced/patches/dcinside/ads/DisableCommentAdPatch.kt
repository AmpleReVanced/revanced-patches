package app.revanced.patches.dcinside.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.ads.fingerprints.postReadCommentAdViewFingerprint
import app.revanced.patches.dcinside.ads.fingerprints.postReadCommentTopAdViewFingerprint
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val disableCommentAdPatch = bytecodePatch(
    name = "Disable Comment Ad",
    description = "Disables the comment ad in the app.",
) {
    compatibleWith("com.dcinside.app.android"("5.2.2"))

    execute {
        val postReadCommentAdViewMethod = postReadCommentAdViewFingerprint.method
        postReadCommentAdViewMethod.apply {
            val returnIndex = implementation!!.instructions.indexOfLast {
                it.opcode == Opcode.RETURN_VOID
            }

            addInstructions(
                returnIndex,
                """
                    const/4 p2, 0x0
                    const/4 p3, 0x0
                    new-instance v0, Landroid/view/ViewGroup${'$'}LayoutParams;
                    invoke-direct {v0, p2, p3}, Landroid/view/ViewGroup${'$'}LayoutParams;-><init>(II)V
                    invoke-virtual {p0, v0}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V

                    const/16 p1, 0x8
                    invoke-virtual {p0, p1}, Landroid/view/View;->setVisibility(I)V

                    iget-object v1, p0, Lcom/dcinside/app/view/PostReadCommentAdView;->f:Ljava/util/concurrent/atomic/AtomicBoolean;
                    const/4 v0, 0x1
                    invoke-virtual {v1, v0}, Ljava/util/concurrent/atomic/AtomicBoolean;->set(Z)V
                """.trimIndent()
            )
        }

        val postReadCommentTopAdViewMethod = postReadCommentTopAdViewFingerprint.method
        postReadCommentTopAdViewMethod.apply {
            val returnIndex = implementation!!.instructions.indexOfLast {
                it.opcode == Opcode.RETURN_VOID
            }

            addInstructions(
                returnIndex,
                """
                    const/4 p2, 0x0
                    const/4 p3, 0x0
                    new-instance v0, Landroid/view/ViewGroup${'$'}LayoutParams;
                    invoke-direct {v0, p2, p3}, Landroid/view/ViewGroup${'$'}LayoutParams;-><init>(II)V
                    invoke-virtual {p0, v0}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V

                    const/16 p1, 0x8
                    invoke-virtual {p0, p1}, Landroid/view/View;->setVisibility(I)V

                    iget-object v1, p0, Lcom/dcinside/app/view/PostReadCommentTopAdView;->c:Ljava/util/concurrent/atomic/AtomicBoolean;
                    const/4 v0, 0x1
                    invoke-virtual {v1, v0}, Ljava/util/concurrent/atomic/AtomicBoolean;->set(Z)V
                """.trimIndent()
            )
        }
    }
}