package app.revanced.patches.dcinside.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.ads.fingerprints.postReadReplyAdViewFingerprint
import app.revanced.patches.dcinside.ads.fingerprints.postReadReplyTopAdViewFingerprint
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val disableCommentAdPatch = bytecodePatch(
    name = "Disable Comment Ad",
    description = "Disables the comment ad in the app.",
) {
    compatibleWith("com.dcinside.app.android"("5.1.6"))

    execute {
        val postReadReplyAdViewMethod = postReadReplyAdViewFingerprint.method
        postReadReplyAdViewMethod.apply {
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

                    const/4 v0, 0x1
                    iput-boolean v0, p0, Lcom/dcinside/app/view/PostReadReplyAdView;->f:Z
                """.trimIndent()
            )
        }

        val postReadReplyTopAdViewMethod = postReadReplyTopAdViewFingerprint.method
        postReadReplyTopAdViewMethod.apply {
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

                    const/4 v0, 0x1
                    iput-boolean v0, p0, Lcom/dcinside/app/view/PostReadReplyTopAdView;->c:Z
                """.trimIndent()
            )
        }
    }
}