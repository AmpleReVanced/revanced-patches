package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val moatCheckResultFingerprintOne = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Ljava/lang/Object;", "Ljava/lang/Object;", "Ljava/lang/Object;")
    returns("Ljava/lang/Object;")
    strings("detectResult", "<unused var>")
    opcodes(
        Opcode.CHECK_CAST,
        Opcode.CHECK_CAST,
        Opcode.CHECK_CAST,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_STATIC,
        Opcode.SGET_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
    )
    custom { method, classDef -> classDef.sourceFile == "PaySecurityWorker.kt" }
}

internal val moatCheckResultFingerprintTwo = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Ljava/lang/Object;", "Ljava/lang/Object;", "Ljava/lang/Object;")
    returns("Ljava/lang/Object;")
    strings("detectResult", "<unused var>")
    opcodes(
        Opcode.CHECK_CAST,
        Opcode.CHECK_CAST,
        Opcode.CHECK_CAST,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_STATIC,
        Opcode.CHECK_CAST,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
        Opcode.SGET_OBJECT,
        Opcode.IF_EQZ,
    )
    custom { method, classDef -> classDef.sourceFile == "PaySecurityWorker.kt" }
}

internal val postprocessMoatCheckFailedFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Lcom/kakaopay/kpsd/moat/sdk/MoatFlag;", "Ljava/lang/String;", "[Ljava/lang/String;")
    strings("msg_title", "msg_body", "OUTPUT_KEY_FAILURE_TITLE", "OUTPUT_KEY_FAILURE_REASON", "ADS_BLOCK은 result message를 사용해야 합니다.", "let(...)", "OUTPUT_KEY_FAILURE_TYPE", "OUTPUT_KEY_PACKAGE_NAMES")
}