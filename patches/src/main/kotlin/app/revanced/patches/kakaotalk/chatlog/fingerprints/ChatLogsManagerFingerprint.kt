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