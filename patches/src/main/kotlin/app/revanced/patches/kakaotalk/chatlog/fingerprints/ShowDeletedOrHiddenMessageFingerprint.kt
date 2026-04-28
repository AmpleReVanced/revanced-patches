package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ChatInfoViewClassFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/talk/widget/chatlog/ChatInfoView;",
)

internal object OthersChatInfoViewClassFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/talk/widget/chatlog/OthersChatInfoView;",
)

internal object MyChatInfoViewClassFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/talk/widget/chatlog/MyChatInfoView;",
)

internal object ChatLogViewHolderSetupChatInfoViewFingerprint : Fingerprint(
    parameters = listOf(),
    returnType = "V",
    strings = listOf("getContext(...)"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.IF_NEZ
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogViewHolder.kt" }
)

internal object CheckViewableChatLogFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
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
        Opcode.RETURN,
        Opcode.CONST_4,
        Opcode.RETURN,
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatLogViewHolder.kt"
                && method.parameterTypes.count() == 1
    }
)

internal object ChatLogItemViewHolderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    strings = listOf("chatLogItem"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.RETURN_OBJECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_4,
        Opcode.RETURN_OBJECT
    ),
    custom = { _, classDef -> classDef.sourceFile == "ViewHolder.kt" }
)

internal object FilterChatLogItemFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_4,
        Opcode.IF_NEZ,
        Opcode.RETURN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE,
        Opcode.RETURN,
        Opcode.INSTANCE_OF
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatLogSearchHelper.kt"
                && method.parameterTypes.size == 1
    }
)
