package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val processWatermarkCountFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("I")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    )
    custom { method, classDef -> classDef.sourceFile == "ChatLogRecyclerItem.kt" }
}

internal val getWatermarkCountFromCacheFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("I")
    strings("chatRoom")
    opcodes(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
    )
    custom { method, classDef -> classDef.sourceFile == "WatermarksManager.kt" }
}