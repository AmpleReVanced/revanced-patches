package app.revanced.patches.chzzk.tongpow

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

private const val STRING = "Ljava/lang/String;"
private const val FLOW = "Lkotlinx/coroutines/flow/Flow;"
private const val SCOPE = "Lkotlinx/coroutines/CoroutineScope;"
private const val JOB = "Lkotlinx/coroutines/Job;"

internal object ShowTongPowPopupInfoFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("ShowTongPowPopupInfo(channelId="),
)

internal object TongPowChatEventFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Lkotlin/Unit;",
    filters = listOf(
        methodCall(definingClass = { ShowTongPowPopupInfoFingerprint.classDef.type }, name = "<init>"),
        methodCall(
            definingClass = "this",
            parameters = { listOf(ShowTongPowPopupInfoFingerprint.classDef.superclass!!) },
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object TongPowManualClaimFingerprint : Fingerprint(
    parameters = emptyList(),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_INTERFACE, parameters = listOf(STRING, STRING)),
        methodCall(parameters = listOf(FLOW), location = MatchAfterWithin(1)),
        methodCall(parameters = emptyList(), returnType = SCOPE, location = MatchAfterWithin(1)),
        methodCall(
            name = "<init>",
            parameters = listOf("Ljava/lang/Object;", "Lkotlin/coroutines/Continuation;", "I"),
            location = MatchAfterWithin(2),
        ),
        methodCall(name = "<init>", parameters = listOf("I"), location = MatchAfterWithin(1)),
        methodCall(
            parameters = listOf(FLOW, SCOPE, "Lkotlin/jvm/functions/Function2;", "Lkotlin/jvm/functions/Function1;"),
            returnType = JOB,
            location = MatchAfterImmediately(),
        ),
        methodCall(parameters = listOf(JOB), returnType = "V", location = MatchAfterWithin(1)),
    ),
)