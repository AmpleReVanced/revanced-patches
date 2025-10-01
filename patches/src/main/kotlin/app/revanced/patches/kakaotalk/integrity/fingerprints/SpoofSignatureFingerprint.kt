package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val utilityGetSignatureFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Landroid/content/Context;")
    returns("Ljava/lang/String;")
    strings("context", "SHA", "encodeToString(md.digest(), Base64.NO_WRAP)")
    opcodes(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    )
    custom { method, classDef -> classDef.sourceFile == "Utility.kt" }
}