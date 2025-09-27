package app.revanced.patches.kakaotalk.member.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val kickButtonManageMethodFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters()
    returns("V")
    strings()
    opcodes(
        Opcode.SGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_DIRECT,
        Opcode.MOVE_RESULT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL
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