package app.revanced.patches.kakaotalk.member

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val alwaysShowKickButtonPatch = bytecodePatch(
    name = "Always Show Kick Button",
    description = "Always shows the kick button in group member management.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val containsUserByIdMethod = ContainsUserByIdFingerprint.method

        KickButtonManageMethodFingerprint.method.apply {
            val index = indexOfFirstInstructionOrThrow(
                methodCall(
                    definingClass = containsUserByIdMethod.definingClass,
                    name = containsUserByIdMethod.name,
                    opcodes = listOf(Opcode.INVOKE_VIRTUAL),
                ),
            )

            val moveResult = getInstruction(index + 1)
            if (moveResult.opcode != Opcode.MOVE_RESULT) {
                throw PatchException("The result of the membership check is not captured.")
            }

            addInstruction(
                index + 2,
                BuilderInstruction11n(Opcode.CONST_4, (moveResult as OneRegisterInstruction).registerA, 0x1),
            )
        }
    }
}