package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object InsertChatLogToChatRoomFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL_RANGE,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IPUT_BOOLEAN,
        Opcode.IGET_OBJECT,
        Opcode.RETURN_OBJECT,
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatRoom.kt" &&
                method.parameterTypes.size == 4 &&
                method.parameterTypes[0] == CHAT_LOG_TYPE &&
                method.parameterTypes[1] == "Z" &&
                method.parameterTypes[3] == "Z"
    },
)

internal object ProcessPendingOverwriteMessagesFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    ),
    custom = { _, classDef ->
        classDef.sourceFile == "PendingOverwriteMessageManager.kt" &&
                classDef.type.contains('$')
    },
)