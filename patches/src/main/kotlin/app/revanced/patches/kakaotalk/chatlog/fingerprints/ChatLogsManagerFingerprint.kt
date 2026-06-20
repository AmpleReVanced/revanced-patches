package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ReplaceToFeedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf(
        "chatLog",
        "feedType",
        "byHost",
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogsManager.kt" }
)

internal object ModifiedChatLogFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf(
        "ModifiedChatLog(chatRoomId=",
        ", chatLogId=",
        ", modifyRevision=",
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "ModifiedChatLog.kt" &&
                method.parameterTypes.isEmpty()
    }
)

internal fun ModifyLogBuilderFingerprint(chatLogType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(chatLogType),
    returnType = "Ljava/lang/String;",
    strings = listOf(
        "message",
        "revision",
        "enc",
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogsManager.kt" }
)

internal fun ModifiedChatLogApplyFingerprint(
    chatLogType: String,
    modifiedChatLogType: String,
) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(chatLogType, modifiedChatLogType, "J"),
    returnType = "V",
    filters = listOf(
        methodCall(
            parameters = listOf(chatLogType),
            returnType = "Ljava/lang/String;",
        ),
        methodCall(
            parameters = listOf(chatLogType, modifiedChatLogType, "Ljava/lang/String;"),
            returnType = chatLogType,
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogsManager.kt" }
)

internal object ChatLogVFieldPutBooleanFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.DECLARED_SYNCHRONIZED),
    parameters = listOf("Ljava/lang/String;", "Z"),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MONITOR_ENTER,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.GOTO,
        Opcode.MOVE_EXCEPTION
    ),
    custom = { _, classDef -> classDef.sourceFile == "VField.kt" && classDef.instanceFields.count() == 1 }
)

internal object ChatLogVFieldPutStringFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.DECLARED_SYNCHRONIZED),
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MONITOR_ENTER,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.GOTO,
        Opcode.MOVE_EXCEPTION
    ),
    custom = { _, classDef -> classDef.sourceFile == "VField.kt" && classDef.instanceFields.count() == 1 }
)

internal object FlushToDBChatLogFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.CONST_4,
        Opcode.RETURN
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatLogsManager.kt"
                && method.parameterTypes.size == 1
    }
)

internal object PutDeletedMessageCacheFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("J", "J"),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.MONITOR_ENTER,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IF_NEZ,
        Opcode.NEW_INSTANCE,
        Opcode.CONST_16,
        Opcode.INVOKE_DIRECT,
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatLogsManager.kt" &&
                method.parameterTypes.size == 2
    }
)

internal object GetDeletedMessageCacheFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Lcom/kakao/talk/db/model/chatlog/c;"),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    ),
    custom = { method, classDef -> classDef.sourceFile == "ChatLogsManager.kt" && method.parameterTypes.size == 1 }
)