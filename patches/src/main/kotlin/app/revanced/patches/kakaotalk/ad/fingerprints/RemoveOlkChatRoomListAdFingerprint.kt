package app.revanced.patches.kakaotalk.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object AddOlkChatRoomListAdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    strings = listOf("call to \'resume\' before \'invoke\' with coroutine"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IF_NE,
        Opcode.INVOKE_STATIC,
        Opcode.GOTO,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
    ),
    custom = { _, classDef -> classDef.sourceFile == "OlkChatRoomListViewModel.kt" }
)

internal object OpenChatTabFragmentAdEnabledFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.SGET_OBJECT,
    ),
    custom = { _, classDef -> classDef.sourceFile == "OpenChatTabFragment.kt" }
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