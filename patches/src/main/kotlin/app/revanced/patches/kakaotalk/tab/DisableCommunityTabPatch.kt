package app.revanced.patches.kakaotalk.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tab.fingerprints.CommonChatRoomListAdapterClassFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.SetupAdapterFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

@Suppress("unused")
val disableCommunityTabPatch = bytecodePatch(
    name = "Disable Community Tab",
    description = "Disables Community Tab",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        SetupAdapterFingerprint.method.apply {
            val fragmentType = SetupAdapterFingerprint.classDef.type

            val setAdapterIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.let { reference ->
                            reference.definingClass == "Landroidx/recyclerview/widget/RecyclerView;" &&
                                    reference.name == "setAdapter" &&
                                    reference.parameterTypes.size == 1 &&
                                    reference.returnType == "V"
                        } == true
            }

            val setAdapterInstruction = getInstruction<FiveRegisterInstruction>(setAdapterIndex)
            val setAdapterReference = setAdapterInstruction.getReference<MethodReference>()
                ?: throw PatchException("Could not inspect OpenChat RecyclerView.setAdapter call")
            val targetAdapterRegister = setAdapterInstruction.registerD
            val recyclerViewAdapterArrayType = "[${setAdapterReference.parameterTypes.single()}"

            val concatAdapterConstructorIndex = indexOfFirstInstructionReversedOrThrow(setAdapterIndex - 1) {
                opcode == Opcode.INVOKE_DIRECT &&
                        (this as? FiveRegisterInstruction)?.registerC == targetAdapterRegister &&
                        getReference<MethodReference>()?.let { reference ->
                            reference.name == "<init>" &&
                                    reference.parameterTypes == listOf(recyclerViewAdapterArrayType) &&
                                    reference.returnType == "V"
                        } == true
            }

            val concatAdapterConstructorInstruction =
                getInstruction<FiveRegisterInstruction>(concatAdapterConstructorIndex)
            val concatAdapterConstructor = concatAdapterConstructorInstruction.getReference<MethodReference>()
                ?: throw PatchException("Could not inspect OpenChat ConcatAdapter constructor")
            val concatAdapterType = concatAdapterConstructor.definingClass
            val adapterArrayRegister = concatAdapterConstructorInstruction.registerD

            val concatAdapterIndex = indexOfFirstInstructionReversedOrThrow(concatAdapterConstructorIndex - 1) {
                opcode == Opcode.NEW_INSTANCE &&
                        (this as? OneRegisterInstruction)?.registerA == targetAdapterRegister &&
                        getReference<TypeReference>()?.type == concatAdapterType
            }

            val adapterSetupConditionIndex = indexOfFirstInstructionReversedOrThrow(concatAdapterIndex - 1) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.let { reference ->
                            reference.definingClass == fragmentType &&
                                    reference.parameterTypes.isEmpty() &&
                                    reference.returnType == "Z"
                        } == true
            }
            if (getInstruction(adapterSetupConditionIndex + 1).opcode != Opcode.MOVE_RESULT ||
                getInstruction(adapterSetupConditionIndex + 2).opcode != Opcode.IF_EQZ
            ) {
                throw PatchException("Could not inspect OpenChat community adapter condition")
            }

            val chatListAdapterType = CommonChatRoomListAdapterClassFingerprint.classDef.type
            val chatListAdapterFieldGetIndex =
                indexOfFirstInstructionOrThrow(adapterSetupConditionIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == chatListAdapterType
                }
            val chatListAdapterFieldGet = getInstruction<TwoRegisterInstruction>(chatListAdapterFieldGetIndex)
            val chatListAdapterField = chatListAdapterFieldGet.getReference<FieldReference>()
                ?: throw PatchException("Could not inspect OpenChat chat list adapter field")
            val chatListAdapterRegister = chatListAdapterFieldGet.registerA
            val fragmentRegister = chatListAdapterFieldGet.registerB

            indexOfFirstInstructionOrThrow(adapterSetupConditionIndex) {
                opcode == Opcode.NEW_ARRAY &&
                        (this as? TwoRegisterInstruction)?.registerA == adapterArrayRegister &&
                        getReference<TypeReference>()?.type == recyclerViewAdapterArrayType
            }

            val zeroRegister = getInstruction<ThreeRegisterInstruction>(
                indexOfFirstInstructionOrThrow(adapterSetupConditionIndex) {
                    if (opcode != Opcode.APUT_OBJECT) {
                        return@indexOfFirstInstructionOrThrow false
                    }

                    (this as? ThreeRegisterInstruction)?.let { instruction ->
                        instruction.registerA == chatListAdapterRegister &&
                                instruction.registerB == adapterArrayRegister
                    } == true
                }
            ).registerC

            removeInstructions(adapterSetupConditionIndex, setAdapterIndex - adapterSetupConditionIndex)
            addInstructions(
                adapterSetupConditionIndex,
                """
                    new-instance v$targetAdapterRegister, $concatAdapterType
                    iget-object v$chatListAdapterRegister, v$fragmentRegister, $chatListAdapterField
                    const/16 v$adapterArrayRegister, 0x1
                    new-array v$adapterArrayRegister, v$adapterArrayRegister, $recyclerViewAdapterArrayType
                    const/16 v$zeroRegister, 0x0
                    aput-object v$chatListAdapterRegister, v$adapterArrayRegister, v$zeroRegister
                    invoke-direct {v$targetAdapterRegister, v$adapterArrayRegister}, $concatAdapterConstructor
                """.trimIndent()
            )
        }
    }
}