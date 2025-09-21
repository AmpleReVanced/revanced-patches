package app.revanced.patches.kakaotalk.ads.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val addOlkChatRoomListAdFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Ljava/lang/Object;")
    returns("Ljava/lang/Object;")
    strings("list", "key_ad_info", "")
    opcodes(
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_STATIC_RANGE,
        Opcode.SGET_BOOLEAN,
        Opcode.IGET_OBJECT,
        Opcode.IGET,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.CONST_STRING,
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
    )
}