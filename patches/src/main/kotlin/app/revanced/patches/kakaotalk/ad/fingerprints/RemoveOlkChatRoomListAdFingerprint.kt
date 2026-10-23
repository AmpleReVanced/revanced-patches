package app.revanced.patches.kakaotalk.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object OpenChatRoomListAdapterFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("L"),
    custom = { _, classDef -> classDef.sourceFile == "OpenChatRoomListAdapter.kt" },
)

internal object CommonChatRoomListSubmitFingerprint : Fingerprint(
    parameters = listOf("Ljava/util/List;", "Lkotlin/jvm/functions/Function0;"),
    returnType = "V",
    custom = { _, classDef -> classDef.sourceFile == "CommonChatRoomListAdapter.kt" },
)

internal object OpenChatAdItemFingerprint : Fingerprint(
    name = "getBindingType",
    parameters = emptyList(),
    returnType = "I",
    custom = { _, classDef -> classDef.sourceFile == "OpenChatAdChatItem.kt" },
)

internal object ChatListAdItemFingerprint : Fingerprint(
    name = "getBindingType",
    parameters = emptyList(),
    returnType = "I",
    custom = { _, classDef -> classDef.sourceFile == "ChatListAdChatItem.kt" },
)

internal object OpenChatTabBizBoardUpdateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/util/List;"),
    returnType = "V",
    filters = listOf(
        fieldAccess(name = "OPEN_CHAT_AD", opcode = Opcode.SGET_OBJECT),
        methodCall(parameters = listOf(), returnType = "Z", opcode = Opcode.INVOKE_VIRTUAL),
        methodCall("Ljava/util/List;->add(ILjava/lang/Object;)V"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "OpenChatTabFragment.kt" },
)