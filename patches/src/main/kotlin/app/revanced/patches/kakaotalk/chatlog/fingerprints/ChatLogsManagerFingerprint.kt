package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val replaceToFeedFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    strings(
        "chatLog",
        "feedType",
        "byHost",
        "no matched overwrite feedType : ",
    )
    custom { method, classDef -> classDef.sourceFile == "ChatLogsManager.kt" }
}

internal val chatLogVFieldPutBooleanFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.DECLARED_SYNCHRONIZED)
    parameters("Ljava/lang/String;", "Z")
    returns("V")
    opcodes(
        Opcode.MONITOR_ENTER,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.GOTO,
        Opcode.MOVE_EXCEPTION
    )
    custom { method, classDef -> classDef.sourceFile == "VField.kt" && classDef.instanceFields.count() == 1 }
}

internal val flushToDBChatLogFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    opcodes(
        Opcode.INSTANCE_OF,
        Opcode.IF_EQZ,
        Opcode.MOVE_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.SGET_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.CONST_4,
        Opcode.RETURN,
    )
    custom { method, classDef -> classDef.sourceFile == "ChatLogsManager.kt" && method.parameterTypes.size == 1 }
}

internal val putDeletedMessageCacheFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("J", "J")
    returns("V")
    opcodes(
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
    )
    custom { method, classDef -> classDef.sourceFile == "ChatLogsManager.kt" }
}

internal val getDeletedMessageCacheFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    opcodes(
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
    )
    custom { method, classDef -> classDef.sourceFile == "ChatLogsManager.kt" && method.parameterTypes.size == 1 }
}