package app.revanced.patches.dcinside.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.dcinside.ad.fingerprints.PostReadImageAdViewFingerprint
import app.revanced.patches.dcinside.ad.fingerprints.RefreshImageAdFingerprint
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val disableImageAdPatch = patch(
    name = "Disable Image Ad",
    description = "Disables the image ad in the app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        PostReadImageAdViewFingerprint.method.apply {
            val index = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.let {
                    it.definingClass == definingClass && it.parameterTypes == listOf("I") && it.returnType == "V"
                } == true
            }
            val heightSetter = getInstruction(index).getReference<MethodReference>()!!
            val view = getInstruction<FiveRegisterInstruction>(index).registerC
            val register = getFreeRegisterProvider(index + 1, 1, view).getFreeRegister4Bit()
            addInstructions(
                index + 1,
                """
                    const/4 v$register, 0x0
                    invoke-direct {v$view, v$register}, $heightSetter
                    const/16 v$register, 0x8
                    invoke-virtual {v$view, v$register}, Landroid/view/View;->setVisibility(I)V
                """,
            )
        }

        RefreshImageAdFingerprint.method.returnEarly()
    }
}
