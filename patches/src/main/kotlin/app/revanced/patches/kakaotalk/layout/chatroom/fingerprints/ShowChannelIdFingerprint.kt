package app.revanced.patches.kakaotalk.layout.chatroom.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.StringComparisonType.CONTAINS
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ChatRoomSideInitFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("J"),
    returnType = "V",
    custom = { _, classDef -> classDef.sourceFile == "ChatRoomSideViewModel.kt" },
)

internal object ChatRoomSideTitleItemLambdaFingerprint : Fingerprint(
    strings = listOf(string("chatRoomSideChatTitleLayout.<anonymous>", CONTAINS)),
    filters = listOf(
        methodCall(
            definingClass = "Lkotlin/jvm/functions/Function4;",
            name = "invoke",
            opcode = Opcode.INVOKE_INTERFACE,
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatRoomSideContent.kt" },
)

internal object ChatRoomProfileEditBindFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Lcom/kakao/talk/widget/ProfileView;",
            name = "loadChatRoom\$default",
        ),
        methodCall(
            parameters = listOf(),
            returnType = "Landroidx/constraintlayout/widget/ConstraintLayout;",
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatRoomProfileEditSettingItem.kt" &&
            method.parameterTypes.size == 1
    },
)

internal object ChatRoomSetIdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("id"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_WIDE,
        Opcode.CMP_LONG,
        Opcode.IF_NEZ,
        Opcode.GOTO,
        Opcode.IPUT_WIDE,
        Opcode.IF_EQZ,
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID,
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatRoom.kt" &&
            method.parameterTypes.size == 2 &&
            method.parameterTypes.last().toString() == "J"
    },
)