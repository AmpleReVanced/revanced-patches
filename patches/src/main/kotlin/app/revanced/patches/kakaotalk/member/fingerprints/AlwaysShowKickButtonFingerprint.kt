package app.revanced.patches.kakaotalk.member.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val alwaysShowKickButtonFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    parameters()
    returns("V")
    strings("getString(...)")
    opcodes(
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.IGET_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.IF_EQ
    )
    custom { method, classDef -> classDef.sourceFile == "OlkProfileFragment.kt" }
}

internal val containsUserByIdFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("J")
    returns("Z")
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    )
    custom { method, classDef -> classDef.sourceFile == "ChatRoom.kt" }
}