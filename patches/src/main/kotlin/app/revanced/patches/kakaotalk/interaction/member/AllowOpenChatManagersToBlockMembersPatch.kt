package app.revanced.patches.kakaotalk.interaction.member

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.patch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val allowOpenChatManagersToBlockMembersPatch = patch(
    name = "Allow Open Chat Managers To Block Members",
    description = "Shows the personal block button for eligible members alongside Open Chat host actions.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        OpenProfileFragmentStaffActionDispatcherFingerprint.apply {
            val blockCall = instructionMatches.first().getInstruction<FiveRegisterInstruction>()
            val kickIndex = instructionMatches.last().index
            method.addInstructionsWithLabels(
                kickIndex + 1,
                """
                    if-eqz v${blockCall.registerD}, :skip_block_button
                    if-eqz v${blockCall.registerE}, :skip_block_button
                    invoke-virtual {v${blockCall.registerC}}, ${OpenProfileFragmentBlockButtonBuilderFingerprint.originalMethod}
                    :skip_block_button
                    nop
                """,
            )
        }
    }
}