package app.revanced.patches.kakaotalk.misc.feature

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object GetFeatureFlagValueFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L"),
    returnType = "Z",
    filters = listOf(
        fieldAccess(type = "Ljava/util/Set;", opcode = Opcode.SGET_OBJECT),
        opcode(Opcode.SGET_BOOLEAN, location = MatchAfterImmediately()),
        fieldAccess(
            type = "Lkotlin/KotlinVersion;",
            opcode = Opcode.SGET_OBJECT,
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.INVOKE_STATIC, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "Feature.kt" }
)

internal object GetFeatureFlagsInExtensionFingerprint : Fingerprint(
    definingClass = "Lapp/revanced/extension/kakaotalk/feature/Flag;",
    name = "getFeatureFlags"
)