package app.revanced.patches.chzzk.tongpow

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import app.revanced.util.localRegisterCount
import app.revanced.util.matches
import app.revanced.util.parameterTypeNames
import app.revanced.util.requireClass
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal data class TongPowAutoClaimInsertion(
    val method: MutableMethod,
    val updatePopupInfoIndex: Int,
    val showPopupIndex: Int,
    val popupViewModelField: FieldReference,
    val chatViewModelField: FieldReference,
    val manualClaim: ManualClaimReferences,
    val channelIdRegister: Int,
    val claimIdRegister: Int,
    val registers: TemporaryRegisters,
) {
    companion object {
        fun resolve(
            classDefsByType: Map<String, ClassDef>,
            popupEventCollectorMethod: MutableMethod,
            manualClaimMethod: MutableMethod,
            receiveAmountMethod: MutableMethod,
        ): TongPowAutoClaimInsertion {
            val method = popupEventCollectorMethod
            val updatePopupInfoIndex = method.findPopupInfoUpdateCallIndex()
            val updatePopupInfoReference = method.instructionMethodReference(updatePopupInfoIndex)
                ?: throw PatchException("Could not inspect TongPow popup update call.")
            val manualClaim = manualClaimMethod.resolveManualClaimReferences(receiveAmountMethod)
            val updateCallRegisters = method.instructionRegisters(updatePopupInfoIndex)
            val channelIdRegister = method.findMoveResultRegisterBefore(
                updatePopupInfoIndex,
                manualClaim.channelIdGetter,
            )
            val claimIdRegister = method.findMoveResultRegisterBefore(
                updatePopupInfoIndex,
                manualClaim.claimIdGetter,
            )

            return TongPowAutoClaimInsertion(
                method = method,
                updatePopupInfoIndex = updatePopupInfoIndex,
                showPopupIndex = method.findPopupTimerCallIndex(
                    updatePopupInfoIndex,
                    updatePopupInfoReference.definingClass,
                ),
                popupViewModelField = method.findLastFieldReferenceBefore(
                    updatePopupInfoIndex,
                    updatePopupInfoReference.definingClass,
                ),
                chatViewModelField = classDefsByType.requireClass(method.definingClass).findFieldByType(
                    manualClaim.callbackConstructor.parameterTypeNames.single(),
                ),
                manualClaim = manualClaim,
                channelIdRegister = channelIdRegister,
                claimIdRegister = claimIdRegister,
                registers = method.reserveTemporaryRegisters(
                    updateCallRegisters,
                    channelIdRegister,
                    claimIdRegister,
                ),
            )
        }
    }
}

internal data class ManualClaimReferences(
    val receiveAmount: MethodReference,
    val hidePopup: MethodReference,
    val callbackConstructor: MethodReference,
    val channelIdGetter: MethodReference,
    val claimIdGetter: MethodReference,
)

internal data class TemporaryRegisters(
    val flag: Int,
    val callback: Int,
    val scratch: Int,
)

private fun Method.resolveManualClaimReferences(receiveAmountMethod: Method): ManualClaimReferences {
    val receiveAmountIndex = instructions.indexOfFirst { instruction ->
        instruction.getReference<MethodReference>()?.matches(receiveAmountMethod) == true
    }
    if (receiveAmountIndex < 0) {
        throw PatchException("Could not find TongPow receive amount call.")
    }

    val receiveAmount = instructions.toList()[receiveAmountIndex].getReference<MethodReference>()
        ?: throw PatchException("Could not inspect TongPow receive amount call.")
    val callbackConstructor = instructions.asSequence()
        .take(receiveAmountIndex)
        .mapNotNull { it.getReference<MethodReference>() }
        .lastOrNull { reference ->
            reference.name == "<init>" &&
                reference.returnType == VOID_TYPE &&
                reference.parameterTypeNames == listOf(parameterTypeNames[2])
        }
        ?: throw PatchException("Could not find TongPow success callback constructor.")
    val hidePopup = instructions.asSequence()
        .drop(receiveAmountIndex + 1)
        .mapNotNull { it.getReference<MethodReference>() }
        .firstOrNull { reference ->
            reference.definingClass == receiveAmountMethod.definingClass &&
                reference.returnType == VOID_TYPE &&
                reference.parameterTypeNames.isEmpty()
        }
        ?: throw PatchException("Could not find TongPow hide popup call.")
    val stringGetters = instructions.asSequence()
        .take(receiveAmountIndex)
        .mapNotNull { it.getReference<MethodReference>() }
        .filter { reference ->
            reference.returnType == STRING_CLASS &&
                reference.parameterTypeNames.isEmpty()
        }
        .toList()
    val channelIdGetter = stringGetters.getOrNull(stringGetters.size - 2)
        ?: throw PatchException("Could not find TongPow channel id getter.")
    val claimIdGetter = stringGetters.lastOrNull()
        ?: throw PatchException("Could not find TongPow claim id getter.")

    if (channelIdGetter.definingClass != claimIdGetter.definingClass) {
        throw PatchException("TongPow channel id and claim id getters belong to different classes.")
    }

    return ManualClaimReferences(
        receiveAmount = receiveAmount,
        hidePopup = hidePopup,
        callbackConstructor = callbackConstructor,
        channelIdGetter = channelIdGetter,
        claimIdGetter = claimIdGetter,
    )
}

private fun Method.findPopupInfoUpdateCallIndex(): Int =
    findPopupInfoUpdateCallIndexOrNull()
        ?: throw PatchException("Could not find TongPow popup update call.")

private fun Method.findPopupTimerCallIndex(
    afterIndex: Int,
    viewModelType: String,
): Int =
    findPopupTimerCallIndexOrNull(afterIndex, viewModelType)
        ?: throw PatchException("Could not find TongPow popup timer call.")

private fun Method.findLastFieldReferenceBefore(
    beforeIndex: Int,
    type: String,
): FieldReference =
    instructions.asSequence()
        .take(beforeIndex)
        .mapNotNull { it.getReference<FieldReference>() }
        .lastOrNull { it.type == type }
        ?: throw PatchException("Could not find field reference for $type.")

private fun ClassDef.findFieldByType(type: String): FieldReference {
    val fields = fields.filter { it.type == type }
    if (fields.size != 1) {
        throw PatchException("Expected one field of type $type in $this, found ${fields.size}.")
    }

    return fields.single()
}

private fun Method.findMoveResultRegisterBefore(
    beforeIndex: Int,
    getter: MethodReference,
): Int {
    val getterIndex = instructions.asSequence()
        .take(beforeIndex)
        .withIndex()
        .lastOrNull { (_, instruction) ->
            instruction.getReference<MethodReference>()?.matches(getter) == true
        }
        ?.index
        ?: throw PatchException("Could not find getter ${getter.smaliReference}.")

    return (instructions.toList().getOrNull(getterIndex + 1) as? OneRegisterInstruction)
        ?.takeIf { it.opcode == Opcode.MOVE_RESULT_OBJECT }
        ?.registerA
        ?: throw PatchException("Could not infer result register for ${getter.smaliReference}.")
}

private fun Method.instructionRegisters(index: Int): List<Int> {
    val instruction = instructions.toList().getOrNull(index)
        ?: throw PatchException("Could not inspect invoke registers in $definingClass->$name.")

    return instruction.registers
        ?: throw PatchException("Could not inspect invoke registers for instruction $index in $definingClass->$name.")
}

private fun MutableMethod.reserveTemporaryRegisters(
    updateCallRegisters: List<Int>,
    channelIdRegister: Int,
    claimIdRegister: Int,
): TemporaryRegisters {
    val candidates = updateCallRegisters
        .drop(1)
        .filter { register ->
            register < localRegisterCount &&
                register != channelIdRegister &&
                register != claimIdRegister
        }
        .distinct()

    if (candidates.size < 3 || candidates.take(3).any { it > 15 }) {
        throw PatchException("Could not reserve low temporary registers for TongPow auto claim.")
    }

    return TemporaryRegisters(
        flag = candidates[0],
        callback = candidates[1],
        scratch = candidates[2],
    )
}

private val Instruction.registers: List<Int>?
    get() = when (this) {
        is FiveRegisterInstruction -> listOf(registerC, registerD, registerE, registerF, registerG)
            .take(registerCount)
        is RegisterRangeInstruction -> (startRegister until startRegister + registerCount).toList()
        else -> null
    }
