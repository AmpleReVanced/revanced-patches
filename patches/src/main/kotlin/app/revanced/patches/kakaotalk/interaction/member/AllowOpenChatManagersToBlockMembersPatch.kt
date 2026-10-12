package app.revanced.patches.kakaotalk.interaction.member

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.cloneMutable
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.misc.extension.addExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/OpenChatMemberActions;"

@Suppress("unused")
val allowOpenChatManagersToBlockMembersPatch = bytecodePatch(
    name = "Allow Open Chat Managers To Block Members",
    description = "Allows open chat room hosts and co-hosts to block regular members from their profiles.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch)

    execute {
        val blockAction = OpenProfileBlindActionFingerprint.run {
            val buttonIndex = instructionMatches[0].index
            val addIndex = instructionMatches[2].index
            val buildAction = instructionMatches[3].instruction.getReference<MethodReference>()!!
            method.cloneMutable(name = "morphe_addBlockButton").apply {
                val buttonRegister = getInstruction<OneRegisterInstruction>(buttonIndex).registerA
                val actionRegister = getInstruction<FiveRegisterInstruction>(addIndex).registerD
                findInstructionIndicesReversedOrThrow(methodCall(definingClass = "Ljava/util/List;", name = "clear"))
                    .forEach { replaceInstruction(it, "nop") }
                replaceInstruction(addIndex, "invoke-interface {v$actionRegister}, $buildAction")
                addInstruction(addIndex + 1, "return-void")
                addInstructions(buttonIndex + 1, """
                    invoke-static {v$buttonRegister}, $EXTENSION_CLASS->getBlockButton(Landroid/widget/TextView;)Landroid/widget/TextView;
                    move-result-object v$buttonRegister
                """)
                addInstructionsWithLabels(0, """
                    if-eqz p1, :morphe_skip
                    if-nez p2, :morphe_block
                    :morphe_skip
                    return-void
                    :morphe_block
                    nop
                """)
                classDef.methods.add(this)
            }
        }

        OpenProfileStaffActionDispatcherFingerprint.method.apply {
            val blockCall = getInstruction<FiveRegisterInstruction>(
                indexOfFirstInstructionOrThrow(methodCall(OpenProfileBlindActionFingerprint.method)),
            )
            val kickIndex = indexOfFirstInstructionOrThrow(methodCall(OpenProfileKickActionFingerprint.method))
            addInstruction(kickIndex + 1,
                "invoke-virtual {v${blockCall.registerC}, v${blockCall.registerD}, v${blockCall.registerE}, v${blockCall.registerF}}, $blockAction",
            )
        }

        openProfileActionsUpdateFingerprint(OpenProfileStaffActionDispatcherFingerprint.method).method
            .addInstruction(0, "invoke-static/range {p0 .. p0}, $EXTENSION_CLASS->hideBlockButton(Landroid/app/Activity;)V")

        OpenProfileFragmentStaffActionDispatcherFingerprint.apply {
            val kickIndex = instructionMatches.last().index
            method.addInstructionsWithLabels(kickIndex + 1, """
                if-eqz p2, :morphe_skip
                if-eqz p3, :morphe_skip
                invoke-virtual {p0}, ${OpenProfileFragmentBlockButtonBuilderFingerprint.method}
                :morphe_skip
                nop
            """)
        }
    }
}