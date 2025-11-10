package app.revanced.patches.kakaotalk.chatroom.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val limit300PlusBaseChatRoomFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters()
    strings("300+")
    custom { method, classDef -> classDef.sourceFile == "BaseChatRoomItem.kt" }
}

internal val limit300PlusOpenChatRoomFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    parameters()
    strings("300+")
    custom { method, classDef -> classDef.sourceFile == "OpenLinkChatsItem.kt" }
}

internal val getUnreadCountFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.DECLARED_SYNCHRONIZED)
    returns("I")
    parameters()
    opcodes(
        Opcode.MONITOR_ENTER,
        Opcode.IGET,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
        Opcode.ADD_INT_2ADDR,
        Opcode.CONST_16
    )
    custom { method, classDef -> classDef.sourceFile == "UnreadCountInfo.kt" }
}