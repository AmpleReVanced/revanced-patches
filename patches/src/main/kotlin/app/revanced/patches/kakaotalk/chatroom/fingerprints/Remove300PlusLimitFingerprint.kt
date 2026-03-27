package app.revanced.patches.kakaotalk.chatroom.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object Limit300PlusBaseChatRoomFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("300+"),
    custom = { _, classDef -> classDef.sourceFile == "BaseChatRoomItemViewHolder.kt" }
)

internal object Limit300PlusOpenChatRoomFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
    strings = listOf("300+"),
    custom = { _, classDef -> classDef.sourceFile == "OpenLinkChatsItem.kt" }
)

internal object GetUnreadCountFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.DECLARED_SYNCHRONIZED),
    returnType = "I",
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MONITOR_ENTER,
        Opcode.IGET,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
        Opcode.ADD_INT_2ADDR,
        Opcode.CONST_16
    ),
    custom = { _, classDef -> classDef.sourceFile == "UnreadCountInfo.kt" }
)