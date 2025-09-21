package app.revanced.patches.kakaotalk.misc.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val checkChatGroupFeatureFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    parameters()
    strings("enable_chatroom_group")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.CONST_STRING,
        Opcode.CONST_4,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ
    )
    custom { method, classDef -> classDef.sourceFile == "OlkSharedPreference.kt" }
}

internal val checkOpenChatTabFeature = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returns("Z")
    parameters()
    strings("channel")
    opcodes(
        Opcode.NEW_INSTANCE,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.INVOKE_DIRECT,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_STATIC
    )
    custom { method, classDef -> classDef.sourceFile == "TalkPreferencesWithBlocking.kt" }
}

internal val listChatRoomSettingsItems = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returns("Ljava/util/ArrayList;")
    parameters("Landroid/content/Context;")
    strings("getString(...)")
    opcodes(
        Opcode.INSTANCE_OF,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.MOVE_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.GOTO
    )
    custom { method, classDef -> classDef.sourceFile == "ChatRoomSettingsActivity.kt" }
}