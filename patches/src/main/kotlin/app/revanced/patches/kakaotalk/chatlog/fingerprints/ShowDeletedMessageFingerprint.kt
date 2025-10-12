package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val chatInfoViewClassFingerprint = fingerprint {
    custom { method, classDef -> classDef.type == "Lcom/kakao/talk/widget/chatlog/ChatInfoView;" }
}

internal val othersChatInfoViewClassFingerprint = fingerprint {
    custom { method, classDef -> classDef.type == "Lcom/kakao/talk/widget/chatlog/OthersChatInfoView;" }
}

internal val myChatInfoViewClassFingerprint = fingerprint {
    custom { method, classDef -> classDef.type == "Lcom/kakao/talk/widget/chatlog/MyChatInfoView;" }
}

internal val chatLogViewHolderSetupChatInfoViewFingerprint = fingerprint {
    parameters()
    returns("V")
    strings("getContext(...)")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
        Opcode.XOR_INT_2ADDR,
        Opcode.INVOKE_STATIC,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
    )
    custom { method, classDef -> classDef.sourceFile == "ChatLogViewHolder.kt" }
}

internal val checkViewableChatLogFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
        Opcode.IF_GT,
        Opcode.CONST_4,
        Opcode.IF_GE,
        Opcode.GOTO,
        Opcode.CONST_4,
        Opcode.RETURN,
    )
    custom { method, classDef -> classDef.sourceFile == "ChatLogViewHolder.kt" && method.parameterTypes.count() == 1 }
}

internal val chatLogItemViewHolderFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters()
    strings("chatLogItem")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.RETURN_OBJECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_4,
        Opcode.RETURN_OBJECT
    )
    custom { method, classDef -> classDef.sourceFile == "ViewHolder.kt" }
}