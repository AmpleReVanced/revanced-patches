package app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ChatLogViewHolderSetupChatInfoViewFingerprint : Fingerprint(
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        methodCall("Lcom/kakao/talk/widget/chatlog/ChatInfoView;->setChatLogId(J)V"),
        methodCall("Lcom/kakao/talk/widget/chatlog/ChatInfoView;->setModify(Z)V"),
        methodCall("Lcom/kakao/talk/widget/chatlog/ChatInfoView;->setModifyTextColor(I)V"),
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
    filters = listOf(
        opcode(Opcode.CONST_4),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(name = "DeletedAll", opcode = Opcode.SGET_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_NE, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN, location = MatchAfterImmediately()),
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatLogSearchHelper.kt"
                && method.parameterTypes.size == 1
    }
)

internal object ChatRoomSyncFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = { listOf(ChatLogFingerprint.originalClassDef.type, "J", "L") },
    returnType = "V",
    filters = listOf(
        methodCall(
            name = "getChatRoomId",
            definingClass = { ChatLogFingerprint.originalClassDef.type },
            returnType = "J",
        ),
        opcode(Opcode.MOVE_RESULT_WIDE, location = MatchAfterImmediately()),
        methodCall(
            definingClass = "this",
            parameters = listOf("J"),
            returnType = "L",
            opcode = Opcode.INVOKE_VIRTUAL,
            location = MatchAfterImmediately(),
        ),
        methodCall(
            parameters = listOf("J"),
            returnType = { ChatLogFingerprint.originalClassDef.type },
            opcode = Opcode.INVOKE_INTERFACE,
        ),
        fieldAccess(name = "DELETE_TO_ALL", opcode = Opcode.SGET_OBJECT),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatRoomListManager.kt" },
) {
    val chatRoomIdGetter get() = instructionMatches[0].methodReference
    val chatRoomLookup get() = instructionMatches[2].methodReference
    val lastChatLogGetter get() = instructionMatches[3].methodReference
}

internal object AccountChatRoomManagerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("J"),
    returnType = { ChatRoomSyncFingerprint.originalClassDef.type },
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("Landroid/content/Context;", "J")),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = emptyList(),
            returnType = { ChatRoomSyncFingerprint.originalClassDef.type },
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "VoxAccountUtil.kt" },
) {
    val provider get() = originalClassDef.staticFields.single { it.type == originalClassDef.type }
}

internal object CurrentOwnerIdFingerprint : Fingerprint(
    classFingerprint = AccountChatRoomManagerFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "J",
)
