package app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val CHAT_LOG_TYPE = "Lcom/kakao/talk/db/model/chatlog/"

internal const val UNIVERSAL_ALIMTALK_MESSAGE_TYPE = "UniversalAlimtalk"

private const val CHAT_LOG_VIEW_HOLDER_TYPE = "Lcom/kakao/talk/activity/chatroom/chatlog/view/holder/"

private const val UNIVERSAL_MESSAGE_TYPE = "Universal"

private const val SHARE_MANAGER_CLASS = "Lcom/kakao/talk/manager/ShareManager;"

internal object ForwardableChatLogFingerprint : Fingerprint(
    definingClass = SHARE_MANAGER_CLASS,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(CHAT_LOG_TYPE),
    filters = List(4) {
        listOf(
            opcode(Opcode.CHECK_CAST),
            methodCall(
                definingClass = CHAT_LOG_TYPE,
                parameters = listOf(),
                returnType = "Z",
                opcode = Opcode.INVOKE_VIRTUAL,
                location = MatchAfterImmediately(),
            ),
        )
    }.flatten(),
)

internal object LinkForwardShortcutFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.CHECK_CAST),
        methodCall(
            definingClass = CHAT_LOG_TYPE,
            parameters = listOf(),
            returnType = "Z",
            opcode = Opcode.INVOKE_VIRTUAL,
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef ->
        classDef.type.startsWith(CHAT_LOG_VIEW_HOLDER_TYPE) &&
            classDef.sourceFile == "ChatLinkViewHolder.kt"
    },
)

internal fun forwardShortcutFingerprint(name: String) = object : Fingerprint(
    name = name,
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = listOf(),
    custom = { _, classDef -> classDef.type.startsWith(CHAT_LOG_VIEW_HOLDER_TYPE) },
) {}

internal object ForwardActionTypeFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(
        methodCall(
            definingClass = SHARE_MANAGER_CLASS,
            parameters = listOf(CHAT_LOG_TYPE),
            returnType = "Z",
        ),
        fieldAccess(name = UNIVERSAL_MESSAGE_TYPE, opcode = Opcode.SGET_OBJECT),
        methodCall(
            smali = "Ljava/util/Set;->add(Ljava/lang/Object;)Z",
            location = MatchAfterImmediately(),
        ),
    ),
)