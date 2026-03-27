package app.revanced.patches.kakaotalk.member

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val alwaysShowKickButtonPatch = bytecodePatch(
    name = "Always Show Kick Button",
    description = "Always shows the kick button in group member management.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val containsUserByIdMethod = ContainsUserByIdFingerprint.method
        val kickButtonManageMethod = KickButtonManageMethodFingerprint.method

        kickButtonManageMethod.instructions.indexOfFirst {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                    it.getReference<MethodReference>()?.name == containsUserByIdMethod.name &&
                    it.getReference<MethodReference>()?.definingClass == containsUserByIdMethod.definingClass
        }.let {
            if (it != -1) {
                val moveResultInst = kickButtonManageMethod.instructions.getOrNull(it + 1) as? OneRegisterInstruction

                if (moveResultInst != null && moveResultInst.opcode == Opcode.MOVE_RESULT) {
                    val register = moveResultInst.registerA

                    kickButtonManageMethod.addInstruction(
                        it + 2,
                        BuilderInstruction11n(
                            Opcode.CONST_4,
                            register,
                            0x1
                        )
                    )
                }
            }
        }
    }
}