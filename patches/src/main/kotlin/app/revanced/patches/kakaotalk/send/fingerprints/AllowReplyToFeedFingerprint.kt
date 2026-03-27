package app.revanced.patches.kakaotalk.send.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object AllowSwipeReplyToFeedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    strings = listOf("null cannot be cast to non-null type com.kakao.talk.db.model.chatlog.LeverageChatLog"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
        Opcode.IF_NEZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogItemTouchHelperCallback.kt" }
)

internal object IsCarouselTypeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_4,
        Opcode.RETURN,
    ),
    custom = { _, classDef -> classDef.sourceFile == "LeverageChatLog.kt" }
)