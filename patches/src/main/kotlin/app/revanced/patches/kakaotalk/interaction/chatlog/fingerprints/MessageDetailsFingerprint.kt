package app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ChatContextMenuActionLabelFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = emptyList(),
    returnType = "Ljava/lang/String;",
    strings = listOf("Undefined"),
    filters = listOf(methodCall("Landroid/content/Context;->getString(I)Ljava/lang/String;")),
    custom = { _, classDef -> classDef.sourceFile == "ChatContextMenuAction.kt" },
)

internal object ChatLogContextMenuActionsFingerprint : Fingerprint(
    name = "invokeSuspend",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall("Lkotlin/collections/CollectionsKt;->plus(Ljava/util/Collection;Ljava/lang/Iterable;)Ljava/util/List;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.NEW_INSTANCE, location = MatchAfterImmediately()),
        methodCall("Ljava/util/ArrayList;-><init>()V", location = MatchAfterImmediately()),
        methodCall("Ljava/util/List;->iterator()Ljava/util/Iterator;", location = MatchAfterImmediately()),
        methodCall(reference = { ChatContextMenuActionLabelFingerprint.originalMethod }),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogContextMenuManager.kt" },
)

internal fun chatLogAdapterBindFingerprint(holderType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(holderType, "I", "Ljava/util/Set;"),
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = holderType, parameters = listOf("L", "Ljava/util/Set;"), returnType = "V"),
        methodCall(definingClass = holderType, parameters = listOf("L"), returnType = "V"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogAdapter.kt" },
)

internal fun senderlessMessageMenuFingerprint(chatLogType: String) = Fingerprint(
    classFingerprint = ChatLogContextMenuActionsFingerprint,
    name = "invokeSuspend",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        methodCall(definingClass = chatLogType, parameters = emptyList(), returnType = "Lcom/kakao/talk/db/model/Friend;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        opcode(Opcode.GOTO_16, location = MatchAfterImmediately()),
    ),
)

internal fun threadMessageActionFilterFingerprint(actionType: String) = object : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(actionType),
    returnType = "Z",
    filters = listOf(opcode(Opcode.INSTANCE_OF), opcode(Opcode.IF_NEZ)),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogContextMenuManager.kt" },
) {}