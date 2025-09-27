package app.revanced.patches.kakaotalk.misc

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.misc.fingerprints.addNavigationTabFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11n
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

@Suppress("unused")
val removeShopTabPatch = bytecodePatch(
    name = "Remove shop tab",
    description = "Removes the shop tab from the bottom navigation bar.",
) {
    compatibleWith("com.kakao.talk"("25.8.2"))

    execute {
        val method = addNavigationTabFingerprint.method

        method.apply {
            // Delete linkedHashMap.put(3, f(3, tabConfig));
            var removeStartIndex = instructions.indexOfFirst {
                it.opcode == Opcode.CONST_4 && (it as Instruction11n).narrowLiteral == 0x3
            }
            if (removeStartIndex >= 0) {
                val removeEndRel = instructions.subList(removeStartIndex, instructions.size).indexOfFirst {
                    it.opcode == Opcode.INVOKE_INTERFACE
                }
                if (removeEndRel >= 0) {
                    val actualEndIndex = removeStartIndex + removeEndRel
                    val count = actualEndIndex - removeStartIndex + 1
                    removeInstructions(removeStartIndex, count)
                }
            }

            // Replace const/4 vX, 0x4 with const/4 vX, 0x3
            val index = instructions.indexOfFirst {
                it.opcode == Opcode.CONST_4 && (it as Instruction11n).narrowLiteral == 0x4
            }

            if (index >= 0) {
                val moveResultObject = instructions[index + 2]

                if (moveResultObject.opcode != Opcode.MOVE_RESULT_OBJECT) {
                    println("Expected MOVE_RESULT_OBJECT at index ${index + 2}, but found ${moveResultObject.opcode}. Aborting replacement.")
                    return@apply
                }

                val register = (moveResultObject as OneRegisterInstruction).registerA

                addInstructions(
                    index + 3,
                    listOf(
                        BuilderInstruction11n(
                            Opcode.CONST_4,
                            register,
                            0x3
                        ),
                        BuilderInstruction35c(
                            Opcode.INVOKE_STATIC,
                            1,
                            register,
                            0,
                            0,
                            0,
                            0,
                            ImmutableMethodReference(
                                "Ljava/lang/Integer;",
                                "valueOf",
                                listOf(
                                    "I"
                                ),
                                "Ljava/lang/Integer;"
                            )
                        ),
                        BuilderInstruction11x(
                            Opcode.MOVE_RESULT_OBJECT,
                            register
                        )
                    )
                )
            }
        }
    }
}