package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fingerprint
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
    custom = { _, classDef -> classDef.sourceFile == "ChatLogsManager.kt" }
)

internal object GetDeletedMessageCacheFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
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
    ),
    custom = { method, classDef -> classDef.sourceFile == "ChatLogsManager.kt" && method.parameterTypes.size == 1 }
)