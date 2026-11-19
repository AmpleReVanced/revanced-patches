package app.revanced.patches.dcinside.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.dcinside.ad.fingerprints.PostReadImageAdViewFingerprint
import app.revanced.patches.dcinside.ad.fingerprints.RefreshImageAdFingerprint
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val disableImageAdPatch = patch(
    name = "Disable Image Ad",
    description = "Disables the image ad in the app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        PostReadImageAdViewFingerprint.method.apply {
            val heightSetter = instructions.mapNotNull { it.getReference<MethodReference>() }.single {
                it.definingClass == definingClass && it.parameterTypes == listOf("I") && it.returnType == "V"
            }
            addInstructions(
                instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID },
                """
                    const/4 p1, 0x0
                    invoke-direct {p0, p1}, $heightSetter
                    const/16 p1, 0x8
                    invoke-virtual {p0, p1}, Landroid/view/View;->setVisibility(I)V
                """,
            )
        }

        RefreshImageAdFingerprint.method.returnEarly()
    }
}