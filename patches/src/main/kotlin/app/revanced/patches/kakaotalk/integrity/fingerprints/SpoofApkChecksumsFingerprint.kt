package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val getApkChecksumsFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Ljava/lang/Object;")
    strings("call to \'resume\' before \'invoke\' with coroutine")
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
        Opcode.GOTO
    )
    custom { method, classDef -> classDef.sourceFile == "AbuseDetectUtil.kt" && method.parameters.size == 2 }
}