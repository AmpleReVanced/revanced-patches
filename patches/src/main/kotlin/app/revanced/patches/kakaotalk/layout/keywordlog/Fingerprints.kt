package app.revanced.patches.kakaotalk.layout.keywordlog

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.Method

internal const val FRIEND_CLASS = "Lcom/kakao/talk/db/model/Friend;"
internal const val PROFILE_VIEW_CLASS = "Lcom/kakao/talk/widget/ProfileView;"
internal const val CHAT_ROOM_PROFILE_DATA_CLASS = "Lcom/kakao/talk/widget/ChatRoomProfileData;"
internal const val SQUIRCLE_DRAWABLE_CLASS = "Lcom/kakao/talk/widget/SquircleBitmapDrawable;"
private const val CHAT_ROOM_PROFILE_DATA_KT_CLASS = "Lcom/kakao/talk/widget/ChatRoomProfileDataKt;"
private const val CHAT_ROOM_PROFILE_OBJECT_CLASS = "Lcom/kakao/talk/widget/ProfileObject\$ChatRoom;"
private const val PROFILE_OBJECT_CLASS = "Lcom/kakao/talk/widget/ProfileObject;"

internal fun keywordMatchCallSiteFingerprint(matchMethod: Method) = Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = matchMethod.definingClass,
            name = matchMethod.name,
            parameters = matchMethod.parameterTypes.map(CharSequence::toString),
            returnType = matchMethod.returnType,
        ),
        checkCast("Ljava/lang/Boolean;"),
        methodCall(
            "Ljava/lang/Boolean;->booleanValue()Z",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
)

internal object KeywordMatchFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L", "Z", "Lkotlin/coroutines/Continuation;"),
    filters = listOf(
        methodCall(name = "getChatRoomId", parameters = listOf(), returnType = "J"),
        methodCall(name = "getUserId", parameters = listOf(), returnType = "J"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "KeywordNotificationManager.kt" },
)

internal object KeywordHighlightFingerprint : Fingerprint(
    classFingerprint = KeywordMatchFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/text/SpannableStringBuilder;",
    parameters = listOf("Landroid/text/SpannableStringBuilder;"),
)

internal object FriendToStringFingerprint : Fingerprint(
    definingClass = FRIEND_CLASS,
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
)

internal object ChatRoomToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("', lastMessage: '", "', lastLogId: '"),
    custom = { _, classDef -> classDef.sourceFile == "ChatRoom.kt" },
)

internal fun chatRoomLastUpdatedAtInMillisFingerprint(chatRoomType: String) = Fingerprint(
    definingClass = chatRoomType,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "J",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            definingClass = chatRoomType,
            type = "I",
            opcode = Opcode.IGET,
        ),
        opcode(Opcode.INT_TO_LONG, location = MatchAfterImmediately()),
        literal(1000L, location = MatchAfterImmediately()),
        opcode(Opcode.MUL_LONG_2ADDR, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN_WIDE, location = MatchAfterImmediately()),
    ),
)

internal fun keywordLogListEnumFingerprint(chatRoomTypeEnum: String) = Fingerprint(
    definingClass = chatRoomTypeEnum,
    name = "<clinit>",
    filters = listOf(
        string("KeywordLogList"),
        fieldAccess(
            definingClass = chatRoomTypeEnum,
            type = chatRoomTypeEnum,
            opcode = Opcode.SPUT_OBJECT,
        ),
    ),
)

internal fun chatRoomTypeFieldFingerprint(chatRoomType: String, chatRoomTypeEnum: String) = Fingerprint(
    definingClass = chatRoomType,
    returnType = chatRoomTypeEnum,
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            definingClass = chatRoomType,
            type = chatRoomTypeEnum,
            opcode = Opcode.IGET_OBJECT,
        ),
    ),
)

internal object ChatRoomListFilterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    parameters = listOf("Ljava/util/List;"),
    custom = { _, classDef -> classDef.sourceFile == "ChatRoomListFilterExtension.kt" },
)

internal fun chatRoomListRefreshFingerprint(viewModelType: String) = Fingerprint(
    definingClass = viewModelType,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(name = "SORT_CHATROOM", opcode = Opcode.SGET_OBJECT),
    ),
)

internal fun generalChatRoomListFilterCallFingerprint(
    viewModelType: String,
    filterMethod: MethodReference,
) = Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = filterMethod.definingClass,
            name = filterMethod.name,
            parameters = listOf("Ljava/util/List;"),
            returnType = "Ljava/util/List;",
        ),
    ),
    custom = { _, classDef -> classDef.type.startsWith(viewModelType.dropLast(1) + "$") },
)

internal fun chatRoomTitleUsageFingerprint(chatRoomType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    filters = listOf(
        methodCall(definingClass = chatRoomType, parameters = listOf(), returnType = "J"),
        methodCall(definingClass = chatRoomType, parameters = listOf(), returnType = "Ljava/lang/String;"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "OpenLinkChatsItem.kt" },
)

internal fun chatRoomDisplayNameUsageFingerprint(chatRoomType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    filters = listOf(
        methodCall(
            definingClass = chatRoomType,
            parameters = listOf("Ljava/util/Set;"),
            returnType = "Ljava/lang/String;",
        ),
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatRoomItem.kt" &&
            method.parameters.size >= 2 &&
            method.parameters[0].toString() == chatRoomType &&
            method.parameters[1].toString() == "Ljava/util/Set;"
    },
)

internal fun chatRoomIntentFingerprint(chatRoomTypeEnum: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/content/Intent;",
    parameters = listOf("Landroid/content/Context;", "J", chatRoomTypeEnum, "Z"),
    custom = { _, classDef -> classDef.type.startsWith("Lcom/kakao/talk/util/IntentUtils$") },
)

internal object ChatRoomProfileFingerprint : Fingerprint(
    definingClass = PROFILE_VIEW_CLASS,
    name = "loadChatRoom",
    returnType = "V",
    parameters = listOf(CHAT_ROOM_PROFILE_DATA_CLASS, "Lcom/kakao/talk/widget/ProfileView\$ImageQuality;"),
    filters = listOf(
        methodCall(
            definingClass = CHAT_ROOM_PROFILE_DATA_KT_CLASS,
            name = "toProfileObject",
            parameters = listOf(CHAT_ROOM_PROFILE_DATA_CLASS),
            returnType = CHAT_ROOM_PROFILE_OBJECT_CLASS,
            opcode = Opcode.INVOKE_STATIC,
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(
            definingClass = PROFILE_VIEW_CLASS,
            type = PROFILE_OBJECT_CLASS,
            opcode = Opcode.IPUT_OBJECT,
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object ChatRoomItemClickFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    name = "onClick",
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        methodCall(parameters = listOf(), returnType = "Lcom/kakao/talk/widget/ViewBindable;"),
        opcode(Opcode.CHECK_CAST),
        methodCall(
            parameters = listOf(),
            returnType = "J",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "BaseChatRoomItemViewHolder.kt" },
)