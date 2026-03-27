package app.revanced.patches.kakaotalk.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object SetupAdapterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.CONST_4,
        Opcode.IGET_OBJECT,
        Opcode.CONST_STRING,
        Opcode.CONST_4,
        Opcode.IF_NEZ,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
    ),
    strings = listOf(
        "binding", "getContext(...)"
    ),
    custom = { _, classDef -> classDef.sourceFile == "OpenChatTabFragment.kt" }
)

internal object InitViewModelFingerprint : Fingerprint(
    name = "initViewModel",
    custom = { _, classDef -> classDef.sourceFile == "OpenChatTabFragment.kt" }
)

internal object CommonChatRoomListAdapterClassFingerprint : Fingerprint(
    custom = { _, classDef ->
        classDef.sourceFile == "CommonChatRoomListAdapter.kt" &&
                !classDef.type.contains("$")
    }
)