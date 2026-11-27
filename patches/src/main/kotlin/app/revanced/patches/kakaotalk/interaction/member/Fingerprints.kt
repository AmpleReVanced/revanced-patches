package app.revanced.patches.kakaotalk.interaction.member

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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

internal object OpenProfileViewerCreateFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/talk/openlink/openprofile/viewer/OpenProfileViewerActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
    filters = listOf(methodCall(name = "onCreate", opcode = Opcode.INVOKE_SUPER)),
)

internal object OlkProfileCreateFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/talk/openlink/profile/OlkProfileActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
    filters = listOf(methodCall(name = "onCreate", opcode = Opcode.INVOKE_SUPER)),
)
