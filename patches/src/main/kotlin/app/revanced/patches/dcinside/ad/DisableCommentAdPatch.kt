package app.revanced.patches.dcinside.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.ad.fingerprints.PostReadCommentAdViewFingerprint
import app.revanced.patches.dcinside.ad.fingerprints.PostReadCommentTopAdViewFingerprint
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val disableCommentAdPatch = bytecodePatch(
    name = "Disable Comment Ad",
    description = "Disables the comment ad in the app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        listOf(PostReadCommentAdViewFingerprint, PostReadCommentTopAdViewFingerprint).forEach { fingerprint ->
            val loadingStateField = fingerprint.classDef.instanceFields.singleOrNull { field ->
                field.type == "Ljava/util/concurrent/atomic/AtomicBoolean;"
            } ?: throw PatchException("Could not infer comment ad loading state field in ${fingerprint.classDef.type}")
            val method = fingerprint.method
            val returnIndex = method.implementation!!.instructions.indexOfLast {
                it.opcode == Opcode.RETURN_VOID
            }

            method.addInstructions(
                returnIndex,
                """
                    const/4 p2, 0x0
                    const/4 p3, 0x0
                    new-instance v0, Landroid/view/ViewGroup${'$'}LayoutParams;
                    invoke-direct {v0, p2, p3}, Landroid/view/ViewGroup${'$'}LayoutParams;-><init>(II)V
                    invoke-virtual {p0, v0}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V

                    const/16 p1, 0x8
                    invoke-virtual {p0, p1}, Landroid/view/View;->setVisibility(I)V

                    iget-object v1, p0, ${loadingStateField.smaliReference}
                    const/4 v0, 0x1
                    invoke-virtual {v1, v0}, Ljava/util/concurrent/atomic/AtomicBoolean;->set(Z)V
                """.trimIndent()
            )
        }
    }
}