package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val checkApkChecksumsFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters()
    returns("Lkotlin/Pair;")
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.MOVE_OBJECT,
        Opcode.CHECK_CAST,
    )
    custom { method, classDef -> classDef.type.startsWith("Lcom/kakaopay/kpsd/moat/sdk/") }
}

internal val moatNativeStatusFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL, AccessFlags.NATIVE)
    parameters("I", "I")
    custom { method, classDef -> method.name == "a" }
}

internal val moatResultClassFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameters("Lcom/kakaopay/kpsd/moat/sdk/MoatFlag\$PrivateMoatFlag;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Z")
    returns("V")
    opcodes(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_BOOLEAN,
        Opcode.RETURN_VOID
    )
    custom { method, classDef -> method.name == "<init>" }
}