package app.revanced.patches.kakaotalk.layout.subdevice

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val ALIMTALK_TYPE_COMPANION_CLASS =
    "Lcom/kakao/talk/bubble/alimtalk/AlimtalkType\$Companion;"

internal const val UNIVERSAL_PUBLIC_DATA_CLASS =
    "Lcom/kakao/talk/bubble/universal/validator/UniversalPublicData;"

private const val VISIBLE_COVER_STATE = "NONE"

internal object AlimtalkCoverStateFingerprint : Fingerprint(
    definingClass = ALIMTALK_TYPE_COMPANION_CLASS,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z"),
    filters = listOf(
        fieldAccess(name = VISIBLE_COVER_STATE, opcode = Opcode.SGET_OBJECT),
    ),
)

internal object LeverageCoverStateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "Z"),
    filters = listOf(
        fieldAccess(name = VISIBLE_COVER_STATE, opcode = Opcode.SGET_OBJECT),
        methodCall(
            definingClass = ALIMTALK_TYPE_COMPANION_CLASS,
            parameters = listOf("Z"),
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
    ),
)

internal object UniversalMobileOnlyFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = UNIVERSAL_PUBLIC_DATA_CLASS,
            parameters = listOf(),
            returnType = "Z",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        opcode(Opcode.NEW_INSTANCE, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "UniversalDataValidator.kt" },
)