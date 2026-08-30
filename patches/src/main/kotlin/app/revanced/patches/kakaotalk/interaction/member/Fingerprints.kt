package app.revanced.patches.kakaotalk.interaction.member

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object OpenProfileStaffActionDispatcherFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "Z", "Z", "Z", "Z", "Z"),
    returnType = "V",
    custom = { _, classDef -> classDef.sourceFile == "OlkOpenProfileViewerActivity.kt" },
)

internal object OpenProfileBlindActionFingerprint : Fingerprint(
    classFingerprint = OpenProfileStaffActionDispatcherFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "Z", "Z"),
    returnType = "V",
    filters = listOf(fieldAccess(name = "text_for_blind", opcode = Opcode.SGET)),
)

internal object OpenProfileKickActionFingerprint : Fingerprint(
    classFingerprint = OpenProfileStaffActionDispatcherFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "Z", "Z"),
    returnType = "V",
    filters = listOf(fieldAccess(name = "text_for_kick_and_report", opcode = Opcode.SGET)),
)

internal object KickButtonManageMethodFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_DIRECT,
        Opcode.MOVE_RESULT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL
    ),
    custom = { _, classDef -> classDef.sourceFile == "OlkProfileFragment.kt" }
)

internal object ContainsUserByIdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("J"),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatRoom.kt" }
)