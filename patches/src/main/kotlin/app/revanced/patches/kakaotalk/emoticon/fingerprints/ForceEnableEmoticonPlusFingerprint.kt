package app.revanced.patches.kakaotalk.emoticon.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val emoticonPlusMeResultConstructorFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameters("Z", "J", "J")
    returns("V")
    opcodes(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.RETURN_VOID
    )
    custom { method, classDef -> classDef.type == "Lcom/kakao/talk/emoticon/itemstore/plus/EmoticonPlusMeResult;" }
}