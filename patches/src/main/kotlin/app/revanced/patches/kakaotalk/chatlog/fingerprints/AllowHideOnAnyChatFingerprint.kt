package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val checkIsAllowedHideFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    strings(
        "chatRoom",
        "chatLog",
    )
    opcodes(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
    )
    custom { method, classDef -> classDef.sourceFile == "DeletableAction.kt" }
}

internal val checkIsEqualWithMyUserIdFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    parameters("J")
    returns("Z")
    strings()
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.CMP_LONG,
        Opcode.IF_NEZ,
        Opcode.CONST_4,
        Opcode.GOTO,
        Opcode.CONST_4,
        Opcode.RETURN
    )
    custom { method, classDef -> classDef.sourceFile == "Me.kt" }
}