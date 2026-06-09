package app.revanced.patches.chzzk.tongpow

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val BOOLEAN_TYPE = "Z"
internal const val CONTINUATION_CLASS = "Lkotlin/coroutines/Continuation;"
internal const val FLOW_COLLECTOR_CLASS = "Lkotlinx/coroutines/flow/FlowCollector;"
internal const val FUNCTION0_CLASS = "Lkotlin/jvm/functions/Function0;"
internal const val INTEGER_CLASS = "Ljava/lang/Integer;"
internal const val LONG_CLASS = "Ljava/lang/Long;"
internal const val OBJECT_CLASS = "Ljava/lang/Object;"
internal const val STATE_CLASS = "Landroidx/compose/runtime/State;"
internal const val STRING_CLASS = "Ljava/lang/String;"
internal const val VOID_TYPE = "V"

private val popupInfoUpdateParameters = listOf(
    STRING_CLASS,
    STRING_CLASS,
    STRING_CLASS,
    STRING_CLASS,
    STRING_CLASS,
    LONG_CLASS,
    INTEGER_CLASS,
    INTEGER_CLASS,
)

private val receiveAmountParameters = listOf(
    STRING_CLASS,
    STRING_CLASS,
    FUNCTION0_CLASS,
)

internal object TongPowReceiveAmountFingerprint : Fingerprint(
    returnType = VOID_TYPE,
    parameters = receiveAmountParameters,
    strings = listOf("channelId", "claimId", "onSuccess"),
)

internal object TongPowManualClaimFingerprint : Fingerprint(
    returnType = BOOLEAN_TYPE,
    parameters = listOf("L", STATE_CLASS, "L"),
    custom = custom@{ method, _ ->
        val instructions = method.implementation?.instructions?.toList()
            ?: return@custom false
        val receiveAmountIndex = instructions.indexOfFirst { instruction ->
            instruction.getReference<MethodReference>()?.isReceiveAmountCall == true
        }

        receiveAmountIndex >= 0 &&
            instructions.asSequence()
                .take(receiveAmountIndex)
                .mapNotNull { it.getReference<MethodReference>() }
                .any { reference ->
                    reference.name == "<init>" &&
                        reference.returnType == VOID_TYPE &&
                        reference.parameterTypeNames == listOf(method.parameterTypeNames[2])
                } &&
            instructions.asSequence()
                .drop(receiveAmountIndex + 1)
                .mapNotNull { it.getReference<MethodReference>() }
                .any { reference ->
                    reference.returnType == VOID_TYPE &&
                        reference.parameterTypeNames.isEmpty()
                }
    },
)

internal object TongPowPopupEventCollectorFingerprint : Fingerprint(
    returnType = OBJECT_CLASS,
    parameters = listOf("L", CONTINUATION_CLASS),
    custom = custom@{ method, classDef ->
        if (FLOW_COLLECTOR_CLASS !in classDef.interfaces) return@custom false

        val updateIndex = method.findPopupInfoUpdateCallIndexOrNull()
            ?: return@custom false
        val updateReference = method.instructionMethodReference(updateIndex)
            ?: return@custom false

        method.findPopupTimerCallIndexOrNull(updateIndex, updateReference.definingClass) != null
    },
)

internal fun Method.findPopupInfoUpdateCallIndexOrNull(): Int? =
    implementation?.instructions?.indexOfFirst { instruction ->
        val reference = instruction.getReference<MethodReference>() ?: return@indexOfFirst false
        reference.returnType == VOID_TYPE &&
            reference.parameterTypeNames == popupInfoUpdateParameters
    }?.takeIf { it >= 0 }

internal fun Method.findPopupTimerCallIndexOrNull(
    afterIndex: Int,
    viewModelType: String,
): Int? {
    val instructions = implementation?.instructions ?: return null
    return instructions.withIndex()
        .drop(afterIndex + 1)
        .firstOrNull { (_, instruction) ->
            val reference = instruction.getReference<MethodReference>() ?: return@firstOrNull false
            reference.definingClass == viewModelType &&
                reference.returnType == VOID_TYPE &&
                reference.parameterTypeNames == listOf(INTEGER_CLASS)
        }
        ?.index
}

internal fun Method.instructionMethodReference(index: Int): MethodReference? =
    implementation?.instructions?.toList()?.getOrNull(index)?.getReference()

internal val MethodReference.parameterTypeNames: List<String>
    get() = parameterTypes.map { it.toString() }

internal val Method.parameterTypeNames: List<String>
    get() = parameterTypes.map { it.toString() }

private val MethodReference.isReceiveAmountCall: Boolean
    get() = returnType == VOID_TYPE &&
        parameterTypeNames == receiveAmountParameters
