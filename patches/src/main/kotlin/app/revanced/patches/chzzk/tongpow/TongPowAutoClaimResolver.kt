package app.revanced.patches.chzzk.tongpow

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.util.getReference
import app.revanced.util.parameterTypeNames
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal data class TongPowClaimInsertion(
    val dispatchIndex: Int,
    val channelRegister: Int,
    val claimRegister: Int,
    val flowRegister: Int,
    val scopeRegister: Int,
    val callbackRegister: Int,
    val constantRegister: Int,
    val serviceField: FieldReference,
    val claimCall: MethodReference,
    val retryCall: MethodReference,
    val scopeCall: MethodReference,
    val callbackConstructor: MethodReference,
    val successConstructor: MethodReference,
    val errorConstructor: MethodReference,
    val collectCall: MethodReference,
    val registerJobCall: MethodReference,
    val callbackCase: Int,
    val errorCase: Int,
)

private const val CLAIM_EVENT_CLASS =
    "Lcom/navercorp/game/android/community/app/ui/overlayplayerend/live/streaming/chat/popup/ChatTongPowEvent\$ShowTongPowPopupInfo;"
private const val TONG_POW_EVENT_CLASS =
    "Lcom/navercorp/game/android/community/app/ui/overlayplayerend/live/streaming/chat/popup/ChatTongPowEvent;"
private const val CLAIM_SERVICE_CLASS =
    "Lcom/navercorp/game/android/community/data/core/service/tongpow/ApiTongPowService\$ApiService;"
private const val API_CALL_CLASS =
    "Lcom/navercorp/game/android/community/data/core/api/coroutine/CoroutineApiCall;"
private const val FLOW_CLASS = "Lkotlinx/coroutines/flow/Flow;"
private const val SCOPE_CLASS = "Lkotlinx/coroutines/CoroutineScope;"
private const val JOB_CLASS = "Lkotlinx/coroutines/Job;"
private const val CONTINUATION_CLASS = "Lkotlin/coroutines/Continuation;"

internal fun resolveTongPowClaimInsertion(
    chatViewModelClass: ClassDef,
    eventMethod: Method,
    manualClaimMethod: Method,
): TongPowClaimInsertion {
    val eventInstructions = eventMethod.instructions.toList()
    val constructorIndex = eventInstructions.indexOfFirst {
        it.getReference<MethodReference>()?.let { reference ->
            reference.definingClass == CLAIM_EVENT_CLASS && reference.name == "<init>"
        } == true
    }.takeIf { it >= 0 } ?: throw PatchException("Could not find TongPow event constructor.")
    val eventRegisters = eventInstructions[constructorIndex].registers
        ?: throw PatchException("Could not inspect TongPow event constructor registers.")
    if (eventRegisters.size != 9 || eventRegisters.take(6).any { it > 15 }) {
        throw PatchException("Unexpected TongPow event constructor registers.")
    }
    val dispatchIndex = eventInstructions.withIndex().drop(constructorIndex + 1).firstOrNull { (_, instruction) ->
        instruction.getReference<MethodReference>()?.let { reference ->
            reference.definingClass == chatViewModelClass.type &&
                reference.parameterTypeNames == listOf(TONG_POW_EVENT_CLASS)
        } == true
    }?.index ?: throw PatchException("Could not find TongPow event dispatch.")

    val manualInstructions = manualClaimMethod.instructions.toList()
    val claimIndex = manualInstructions.indexOfFirst {
        it.getReference<MethodReference>()?.let { reference ->
            reference.definingClass == CLAIM_SERVICE_CLASS &&
                reference.parameterTypeNames == listOf("Ljava/lang/String;", "Ljava/lang/String;") &&
                reference.returnType == API_CALL_CLASS
        } == true
    }.takeIf { it >= 0 } ?: throw PatchException("Could not find TongPow claim API call.")
    val collectIndex = manualInstructions.indexOfFirst {
        it.getReference<MethodReference>()?.let { reference ->
            reference.parameterTypeNames == listOf(
                FLOW_CLASS, SCOPE_CLASS, "Lkotlin/jvm/functions/Function2;", "Lkotlin/jvm/functions/Function1;",
            ) && reference.returnType == JOB_CLASS
        } == true
    }.takeIf { it > claimIndex } ?: throw PatchException("Could not find TongPow claim collection.")

    fun referenceAt(index: Int) = manualInstructions[index].getReference<MethodReference>()
        ?: throw PatchException("Could not inspect TongPow claim instruction " + index)
    fun findReference(start: Int, end: Int, predicate: (MethodReference) -> Boolean) =
        (start until end).firstOrNull { index ->
            manualInstructions[index].getReference<MethodReference>()?.let(predicate) == true
        } ?: throw PatchException("Could not resolve TongPow claim reference.")

    val callbackIndex = findReference(0, collectIndex) {
        it.name == "<init>" && it.parameterTypeNames == listOf(chatViewModelClass.type, "I")
    }
    val callbackConstructor = referenceAt(callbackIndex)
    val successIndex = findReference(callbackIndex + 1, collectIndex) {
        it.name == "<init>" && it.parameterTypeNames == listOf(callbackConstructor.definingClass, CONTINUATION_CLASS)
    }
    val errorIndex = findReference(successIndex + 1, collectIndex) {
        it.name == "<init>" && it.parameterTypeNames == listOf("I")
    }
    fun caseBefore(index: Int): Int {
        val register = manualInstructions[index].registers?.lastOrNull()
            ?: throw PatchException("Could not inspect TongPow callback constructor.")
        return manualInstructions.subList(0, index).lastOrNull { instruction ->
            instruction is OneRegisterInstruction && instruction.registerA == register &&
                instruction is NarrowLiteralInstruction
        }?.let { (it as NarrowLiteralInstruction).narrowLiteral }
            ?: throw PatchException("Could not resolve TongPow callback case.")
    }

    val serviceField = chatViewModelClass.fields.singleOrNull { it.type == CLAIM_SERVICE_CLASS }
        ?: throw PatchException("Could not find TongPow claim service field.")

    return TongPowClaimInsertion(
        dispatchIndex = dispatchIndex,
        channelRegister = eventRegisters[1],
        claimRegister = eventRegisters[3],
        flowRegister = eventRegisters[0],
        scopeRegister = eventRegisters[2],
        callbackRegister = eventRegisters[4],
        constantRegister = eventRegisters[5],
        serviceField = serviceField,
        claimCall = referenceAt(claimIndex),
        retryCall = referenceAt(findReference(claimIndex + 1, collectIndex) {
            it.parameterTypeNames == listOf(FLOW_CLASS)
        }),
        scopeCall = referenceAt(findReference(claimIndex + 1, collectIndex) {
            it.parameterTypeNames.isEmpty() && it.returnType == SCOPE_CLASS
        }),
        callbackConstructor = callbackConstructor,
        successConstructor = referenceAt(successIndex),
        errorConstructor = referenceAt(errorIndex),
        collectCall = referenceAt(collectIndex),
        registerJobCall = referenceAt(findReference(collectIndex + 1, manualInstructions.size) {
            it.parameterTypeNames == listOf(JOB_CLASS) && it.returnType == "V"
        }),
        callbackCase = caseBefore(callbackIndex),
        errorCase = caseBefore(errorIndex),
    )
}

private val Instruction.registers: List<Int>?
    get() = when (this) {
        is FiveRegisterInstruction -> listOf(registerC, registerD, registerE, registerF, registerG).take(registerCount)
        is RegisterRangeInstruction -> (startRegister until startRegister + registerCount).toList()
        else -> null
    }