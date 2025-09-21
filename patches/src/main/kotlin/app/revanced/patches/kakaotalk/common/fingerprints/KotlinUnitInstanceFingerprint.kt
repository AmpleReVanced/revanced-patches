package app.revanced.patches.kakaotalk.common.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val kotlinUnitInstanceFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters()
    returns("Ljava/lang/String;")
    strings("kotlin.Unit")
    opcodes(
        Opcode.CONST_STRING,
        Opcode.RETURN_OBJECT
    )
    custom { method, classDef ->
        method.name == "toString"
    }
}