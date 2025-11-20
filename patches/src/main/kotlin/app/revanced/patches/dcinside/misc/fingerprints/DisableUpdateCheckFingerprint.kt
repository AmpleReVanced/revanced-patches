package app.revanced.patches.dcinside.misc.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val disableUpdateCheckFingerprint = fingerprint {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    parameters("Ljava/lang/String;")
    strings("null cannot be cast to non-null type androidx.appcompat.app.AppCompatActivity")
    opcodes(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL
    )
    custom { method, classDef -> classDef.type.startsWith("Lcom/dcinside/app/main") }
}