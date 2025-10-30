package app.revanced.patches.kakaotalk.member

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.member.fingerprints.containsUserByIdFingerprint
import app.revanced.patches.kakaotalk.member.fingerprints.kickButtonManageMethodFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val alwaysShowKickButtonPatch = bytecodePatch(
    name = "Always Show Kick Button",
    description = "Always shows the kick button in group member management.",
) {
    compatibleWith("com.kakao.talk"("25.9.1"))

    execute {
        val containsUserByIdMethod = containsUserByIdFingerprint.method
        val kickButtonManageMethod = kickButtonManageMethodFingerprint.method

        kickButtonManageMethod.instructions.indexOfFirst {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                    it.getReference<MethodReference>()?.name == containsUserByIdMethod.name &&
                    it.getReference<MethodReference>()?.definingClass == containsUserByIdMethod.definingClass
        }.let {
            val moveInst = kickButtonManageMethod.instructions.getOrNull(it + 2) as TwoRegisterInstruction
            val register = moveInst.registerA

            kickButtonManageMethod.addInstruction(
                it + 3,
                BuilderInstruction11n(
                    Opcode.CONST_4,
                    register,
                    0x1
                )
            )
        }
    }
}