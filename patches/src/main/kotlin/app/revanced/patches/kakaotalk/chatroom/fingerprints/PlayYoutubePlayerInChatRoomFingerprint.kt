package app.revanced.patches.kakaotalk.chatroom.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object IsPlayYoutubePlayerInChatRoomFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    strings = listOf("youtubePlayerInChatRoom"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_4
    ),
    custom = { method, classDef -> classDef.sourceFile == "CbtPreferences.kt" }
)