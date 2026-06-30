package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.methodCall
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

internal object ChatLogViewHolderBindProfileFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "V",
    filters = listOf(
        methodCall("Lcom/kakao/talk/widget/ProfileView;->load(JLjava/lang/String;I)V"),
        methodCall("Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogViewHolder.kt" },
)

internal object CheckViewableChatLogFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Lcom/kakao/talk/db/model/chatlog/c;"),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatLogViewHolder.kt"
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

internal object ChatRoomListManagerGetInstanceFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    strings = listOf("sInstance"),
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.IF_NEZ,
        Opcode.CONST_CLASS,
        Opcode.MONITOR_ENTER,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatRoomListManager.kt" }
)

internal object GetChatRoomByChannelIdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("J"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.MOVE_OBJECT,
        Opcode.MOVE_WIDE,
        Opcode.INVOKE_STATIC_RANGE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.RETURN_OBJECT
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatRoomListManager.kt" }
)

internal object OriginalSyncMethodFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("chatLog", "feedType"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_NEZ,
        Opcode.RETURN_VOID
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatRoomListManager.kt" }
)
