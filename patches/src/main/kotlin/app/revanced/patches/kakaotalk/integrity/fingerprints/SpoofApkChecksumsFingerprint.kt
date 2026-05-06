package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object GetApkChecksumsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    strings = listOf("call to \'resume\' before \'invoke\' with coroutine"),
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
        Opcode.GOTO
    ),
    custom = { method, classDef -> classDef.sourceFile == "AbuseDetectUtil.kt" && method.parameters.size == 2 }
)