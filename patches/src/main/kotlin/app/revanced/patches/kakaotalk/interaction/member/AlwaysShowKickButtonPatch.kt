package app.revanced.patches.kakaotalk.interaction.member

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val alwaysShowKickButtonPatch = bytecodePatch(
    name = "Always Show Kick Button",
    description = "Always shows the kick button in group member management.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val kickButtonBuilder = KickButtonBuilderFingerprint.method
        listOf(
            kickButtonEligibilityFingerprint(kickButtonBuilder, 2),
            kickButtonEligibilityFingerprint(kickButtonBuilder),
            OpenProfileHostKickActionFingerprint,
            OpenProfileKickActionFingerprint,
        ).forEach { fingerprint ->
            fingerprint.method.apply {
                val membershipRegister = implementation!!.registerCount - parameterTypes.size
                removeInstruction(indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IF_EQZ && (this as OneRegisterInstruction).registerA == membershipRegister
                })
            }
        }
    }
}