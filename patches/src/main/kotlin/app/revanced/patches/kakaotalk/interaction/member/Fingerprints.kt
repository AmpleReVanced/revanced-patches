package app.revanced.patches.kakaotalk.interaction.member

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object OpenProfileStaffActionDispatcherFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "Z", "Z", "Z", "Z", "Z"),
    returnType = "V",
    custom = { _, classDef -> classDef.sourceFile == "OlkOpenProfileViewerActivity.kt" },
)

internal fun openProfileActionsUpdateFingerprint(dispatcher: MethodReference) = Fingerprint(
    classFingerprint = OpenProfileStaffActionDispatcherFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = listOf(methodCall(dispatcher)),
)

internal object OpenProfileBlindActionFingerprint : Fingerprint(
    classFingerprint = OpenProfileStaffActionDispatcherFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "Z", "Z"),
    returnType = "V",
    filters = listOf(
        fieldAccess(type = "Lcom/kakao/talk/theme/widget/ThemeTextView;", opcode = Opcode.IGET_OBJECT),
        fieldAccess(name = "text_for_blind", opcode = Opcode.SGET),
        methodCall(definingClass = "Ljava/util/List;", name = "add"),
        methodCall(parameters = listOf(), returnType = "V", opcode = Opcode.INVOKE_INTERFACE),
    ),
)

internal object OpenProfileKickActionFingerprint : Fingerprint(
    classFingerprint = OpenProfileStaffActionDispatcherFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "Z", "Z"),
    returnType = "V",
    filters = listOf(fieldAccess(name = "text_for_kick_and_report", opcode = Opcode.SGET)),
)

internal object OpenProfileHostKickActionFingerprint : Fingerprint(
    classFingerprint = OpenProfileStaffActionDispatcherFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "Z"),
    returnType = "V",
    filters = listOf(fieldAccess(name = "text_for_kick_and_report", opcode = Opcode.SGET)),
)

internal object KickButtonBuilderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        fieldAccess(name = "profile_home_btn_openkickout", opcode = Opcode.SGET),
        fieldAccess(name = "text_for_kick_and_report", opcode = Opcode.SGET),
    ),
    custom = { _, classDef -> classDef.sourceFile == "OlkProfileFragment.kt" },
)

internal object OpenProfileFragmentBlockButtonBuilderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        fieldAccess(name = "profile_home_btn_openblock", opcode = Opcode.SGET),
        fieldAccess(name = "text_for_blind", opcode = Opcode.SGET),
    ),
    custom = { _, classDef -> classDef.sourceFile == "OlkProfileFragment.kt" },
)

internal object OpenProfileFragmentStaffActionDispatcherFingerprint : Fingerprint(
    classFingerprint = OpenProfileFragmentBlockButtonBuilderFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "Z", "Z", "Z", "Z"),
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "this",
            parameters = listOf("Z", "Z", "Z"),
            returnType = "V",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
        methodCall(
            definingClass = "this",
            parameters = listOf("Z", "Z", "Z"),
            returnType = "V",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
    ),
)

internal fun kickButtonEligibilityFingerprint(kickButtonBuilder: MethodReference, parameterCount: Int = 3) = Fingerprint(
    classFingerprint = KickButtonBuilderFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = List(parameterCount) { "Z" },
    returnType = "V",
    filters = listOf(
        opcode(Opcode.IF_EQZ, InstructionLocation.MatchFirst()),
        methodCall(kickButtonBuilder),
    ),
)