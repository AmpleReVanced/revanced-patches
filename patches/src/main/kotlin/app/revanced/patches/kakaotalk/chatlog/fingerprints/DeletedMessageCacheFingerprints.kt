package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

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
    },
)

internal object GetDeletedMessageCacheFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(CHAT_LOG_TYPE),
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
    custom = { method, classDef ->
        classDef.sourceFile == "ChatLogsManager.kt" &&
                method.parameterTypes.size == 1
    },
)