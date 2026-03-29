package app.revanced.patches.kakaotalk.tracker.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

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
        Opcode.INVOKE_DIRECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IF_EQ,
        Opcode.IF_EQ,
        Opcode.IF_EQ,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST
    ),
    custom = { _, classDef -> classDef.sourceFile == "S2EventRepository.kt" }
)