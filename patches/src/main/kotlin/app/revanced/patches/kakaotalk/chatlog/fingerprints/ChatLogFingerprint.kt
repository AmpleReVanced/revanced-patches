package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val chatLogFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Ljava/lang/String;")
    parameters()
    strings(
        "[class:",
        "] ChatLog [id=",
    )
}

internal val chatLogSetTextFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    parameters("Ljava/lang/String;")
    strings("\u202e", "")
    opcodes(
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.CONST_16,
        Opcode.IF_LE,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
    )
    custom { method, classDef -> classDef.sourceFile == "ChatLog.kt" }
}

internal val chatLogGetTextFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Ljava/lang/String;")
    parameters()
    opcodes(
        Opcode.IGET_BOOLEAN,
        Opcode.CONST_STRING,
        Opcode.IF_EQZ,
        Opcode.GOTO,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.GOTO,
        Opcode.MOVE_OBJECT,
    )
    custom { method, classDef -> classDef.sourceFile == "ChatLog.kt" }
}