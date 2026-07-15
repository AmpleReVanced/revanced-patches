package app.revanced.patches.kakaotalk.tracker.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val S2_EVENT_RESPONSE_CLASS =
    "Lcom/kakao/talk/core/tracker/data/model/S2EventResponse;"

internal object DisableSaveS2EventFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    strings = listOf(
        "call to 'resume' before 'invoke' with coroutine",
        "AllDone"
    ),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INSTANCE_OF,
        Opcode.IF_EQZ,
        Opcode.MOVE_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IGET,
        Opcode.CONST_HIGH16,
        Opcode.AND_INT,
        Opcode.IF_EQZ,
        Opcode.SUB_INT_2ADDR,
        Opcode.IPUT,
        Opcode.GOTO,
        Opcode.NEW_INSTANCE,
    ),
    custom = { _, classDef -> classDef.sourceFile == "Tracker.kt" }
)

internal object SendS2EventFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    strings = listOf(
        "call to \'resume\' before \'invoke\' with coroutine"
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "S2EventRepository.kt" &&
            method.implementation?.instructions?.any { instruction ->
                instruction.getReference<MethodReference>()?.definingClass == S2_EVENT_RESPONSE_CLASS
            } == true
    },
)