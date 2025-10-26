package app.revanced.patches.kakaotalk.tracker.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val disableSaveS2EventFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Ljava/lang/Object;")
    strings(
        "call to 'resume' before 'invoke' with coroutine",
        "AllDone"
    )
    opcodes(
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
    )
    custom { _, classDef -> classDef.sourceFile == "Tracker.kt" }
}

internal val sendS2EventFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Ljava/lang/Object;")
    strings(
        "call to \'resume\' before \'invoke\' with coroutine"
    )
    opcodes(
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
    )
    custom { _, classDef -> classDef.sourceFile == "S2EventRepository.kt" }
}