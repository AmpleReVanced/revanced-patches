package app.revanced.patches.dcinside.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.revanced.patches.dcinside.ad.fingerprints.commentAdConstructorFingerprint
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val disableCommentAdPatch = patch(
    name = "Disable Comment Ad",
    description = "Disables the comment ad in the app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        listOf("Top", "Center", "Bottom").forEach { position ->
            commentAdConstructorFingerprint(position).apply {
                val loadingState = classDef.instanceFields.singleOrNull {
                    it.type == "Ljava/util/concurrent/atomic/AtomicBoolean;"
                }?.let {
                    """
                        iget-object p2, p0, ${it.smaliReference}
                        const/4 p3, 0x1
                        invoke-virtual {p2, p3}, Ljava/util/concurrent/atomic/AtomicBoolean;->set(Z)V
                    """
                }.orEmpty()
                method.addInstructions(
                    method.implementation!!.instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID },
                    """
                        const/4 p1, 0x0
                        new-instance p2, Landroid/view/ViewGroup${'$'}LayoutParams;
                        invoke-direct {p2, p1, p1}, Landroid/view/ViewGroup${'$'}LayoutParams;-><init>(II)V
                        invoke-virtual {p0, p2}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V
                        const/16 p1, 0x8
                        invoke-virtual {p0, p1}, Landroid/view/View;->setVisibility(I)V
                        $loadingState
                    """,
                )
            }
        }
    }
}