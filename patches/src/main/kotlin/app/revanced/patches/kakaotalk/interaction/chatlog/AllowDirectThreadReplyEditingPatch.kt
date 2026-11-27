package app.revanced.patches.kakaotalk.interaction.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.patch
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ModifyMessageActionConstructorFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val allowDirectThreadReplyEditingPatch = patch(
    name = "Allow direct thread reply editing",
    description = "Shows the edit action when long-pressing your own thread replies in the main chat.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        ModifyMessageActionConstructorFingerprint.apply {
            method.removeInstructions(instructionMatches.first().index, instructionMatches.size)
        }
    }
}