package app.revanced.patches.kakaotalk.emoticon.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val isEnableEmoticonPlusFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    parameters()
    strings("emoticonPlusMe")
    opcodes(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.SGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.IGET_BOOLEAN,
        Opcode.RETURN,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_4,
        Opcode.THROW,
    )
}