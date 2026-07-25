package app.revanced.patches.kakaotalk.chatroom.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ChatRoomSideInitFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("chatId"),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatRoomSideActivity.kt" &&
            method.parameterTypes.isEmpty()
    },
)

internal object ChatRoomSideTitleItemLambdaFingerprint : Fingerprint(
    strings = listOf("\$this\$item"),
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
    strings = listOf("profileEdit", "getRoot(...)"),
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