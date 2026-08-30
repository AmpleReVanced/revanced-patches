package app.revanced.patches.kakaotalk.interaction.member

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c

private fun BuilderInstruction35c.copy() = BuilderInstruction35c(
    opcode,
    registerCount,
    registerC,
    registerD,
    registerE,
    registerF,
    registerG,
    reference,
)

@Suppress("unused")
val allowOpenChatManagersToBlockMembersPatch = bytecodePatch(
    name = "Allow Open Chat Managers To Block Members",
    description = "Allows open chat room hosts and co-hosts to block regular members from their profiles.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val blindAction = OpenProfileBlindActionFingerprint.method
        val kickAction = OpenProfileKickActionFingerprint.method

        OpenProfileStaffActionDispatcherFingerprint.method.apply {
            val blindCall = getInstruction<BuilderInstruction35c>(
                indexOfFirstInstructionOrThrow(methodCall(blindAction)),
            )
            val kickCallIndex = indexOfFirstInstructionOrThrow(methodCall(kickAction))

            replaceInstruction(
                kickCallIndex,
                blindCall.copy(),
            )
        }

        OpenProfileFragmentStaffActionDispatcherFingerprint.apply {
            method.apply {
                val blockCall = getInstruction<BuilderInstruction35c>(instructionMatches.first().index)
                val kickCallIndex = instructionMatches.last().index
                val kickCall = getInstruction<BuilderInstruction35c>(kickCallIndex)

                replaceInstruction(kickCallIndex, blockCall.copy())
                addInstruction(kickCallIndex + 1, kickCall)
            }
        }
    }
}