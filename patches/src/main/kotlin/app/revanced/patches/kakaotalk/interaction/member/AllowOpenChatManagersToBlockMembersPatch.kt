package app.revanced.patches.kakaotalk.interaction.member

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c

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
                BuilderInstruction35c(
                    blindCall.opcode,
                    blindCall.registerCount,
                    blindCall.registerC,
                    blindCall.registerD,
                    blindCall.registerE,
                    blindCall.registerF,
                    blindCall.registerG,
                    blindCall.reference,
                ),
            )
        }
    }
}