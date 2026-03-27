package app.revanced.patches.kakaotalk.tab.fingerprints

import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val mainTabConfigFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameters("Z", "Z", "Z", "Z", "Z", "Z", "Ljava/lang/String;")
    returns("V")
    opcodes(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_OBJECT,
        Opcode.RETURN_VOID,
    )
    custom { method, classDef -> classDef.sourceFile == "MainTabConfig.kt" }
}

internal val isHideFriendsTabSettingsFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters()
    returns("Z")
    opcodes(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    )
    custom { method, classDef -> classDef.sourceFile == "FriendTab.kt" }
}

internal val determineFeedOrListMethodFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Ljava/lang/Object;")
    returns("Ljava/lang/Object;")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IF_NE,
        Opcode.IGET_BOOLEAN,
        Opcode.IGET_BOOLEAN,
        Opcode.IGET_BOOLEAN,
        Opcode.INVOKE_STATIC,
        Opcode.GOTO,
        Opcode.NEW_INSTANCE,
        Opcode.CONST_STRING,
        Opcode.INVOKE_DIRECT,
        Opcode.THROW,
        Opcode.INVOKE_STATIC,
        Opcode.IGET_BOOLEAN,
        Opcode.IGET_BOOLEAN,
        Opcode.IGET_BOOLEAN
    )
    custom { method, classDef -> classDef.sourceFile == "FriendSettingsViewModel.kt" }
}