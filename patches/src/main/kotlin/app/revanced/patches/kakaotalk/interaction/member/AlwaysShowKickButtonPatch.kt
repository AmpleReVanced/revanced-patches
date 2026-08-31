package app.revanced.patches.kakaotalk.interaction.member

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val alwaysShowKickButtonPatch = bytecodePatch(
    name = "Always Show Kick Button",
    description = "Always shows the kick button in group member management.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        kickButtonEligibilityFingerprint(KickButtonBuilderFingerprint.method).apply {
            method.removeInstruction(instructionMatches.first().index)
        }
    }
}