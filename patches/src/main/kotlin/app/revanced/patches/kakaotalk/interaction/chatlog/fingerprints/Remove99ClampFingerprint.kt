package app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ProcessWatermarkCountFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "I",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogRecyclerItem.kt" }
)

internal object GetWatermarkCountFromCacheFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "J", "J"),
    returnType = "I",
    filters = listOf(
        literal(99, opcodes = listOf(Opcode.CONST_16)),
        opcode(Opcode.IF_LE, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "WatermarksManager.kt" }
)
