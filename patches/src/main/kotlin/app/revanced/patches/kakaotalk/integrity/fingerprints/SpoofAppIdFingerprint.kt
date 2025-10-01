package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val getAppIdFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    parameters("Landroid/content/Context;")
    returns("Ljava/lang/String;")
    strings("@\u0015s1w\u0000N4", "??-98", "OnePassManager")
    opcodes(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    )
    custom { method, classDef -> classDef.sourceFile == "OnePassManager.java" && method.name == "GetAppID" }
}

internal val uaffacetidMethodFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    parameters("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;")
    returns("Ljava/lang/String;")
    custom { method, classDef -> classDef.sourceFile == "UAFFacetID.java" }
}