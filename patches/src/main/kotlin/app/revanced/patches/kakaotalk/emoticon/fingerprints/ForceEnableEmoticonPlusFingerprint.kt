package app.revanced.patches.kakaotalk.emoticon.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val isEnableEmoticonPlusFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    parameters()
    strings()
    opcodes(
        Opcode.SGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.MOVE,
        Opcode.RETURN
    )
    custom { method, classDef -> classDef.sourceFile == "EmoticonPlusManager.kt" }
}