package app.revanced.patches.soop.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.findFreeRegister
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.soop.ads.fingerprints.*
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val removeExitAdPatch = bytecodePatch(
    name = "Remove exit dialog ad",
    description = "Removes the ad area shown in the app-exit confirmation dialog, keeping the dialog itself.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        fun MutableMethod.hideAdAreaAfterReveal() {
            val revealIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "setVisibility"
            }
            val viewRegister = getInstruction<FiveRegisterInstruction>(revealIndex).registerC
            val freeRegister = findFreeRegister(revealIndex + 1, viewRegister)

            addInstructions(
                revealIndex + 1,
                """
                    const/16 v$freeRegister, 0x8
                    invoke-virtual { v$viewRegister, v$freeRegister }, Landroid/view/View;->setVisibility(I)V
                """,
            )
        }

        ExitAdRevealFingerprint.method.hideAdAreaAfterReveal()
        ExitAdAnimateRevealFingerprint.method.hideAdAreaAfterReveal()
    }
}