package app.revanced.patches.kakaotalk.misc.spoof.versioninfo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object VersionInfoFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Lkotlin/Unit;",
    filters = listOf(
        opcode(Opcode.XOR_INT_LIT8),
        string({ packageMetadata.versionName }),
        methodCall(parameters = listOf("Ljava/lang/String;", "Z", "Lkotlin/jvm/functions/Function0;")),
    ),
    custom = { _, classDef -> classDef.sourceFile == "AppAdminSettingsActivity.kt" },
)

internal object VersionInfoPreviewFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        opcode(Opcode.IF_EQZ),
        string({ packageMetadata.versionName }, MatchAfterImmediately()),
        opcode(Opcode.RETURN_OBJECT, MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "SettingActivity.kt" },
)

internal object AdaptiveVersionInfoFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "Z"),
    returnType = "L",
    filters = listOf(
        opcode(Opcode.NEW_INSTANCE),
        string({ packageMetadata.versionName }),
        methodCall(
            name = "<init>",
            parameters = listOf("Ljava/lang/String;"),
            opcode = Opcode.INVOKE_DIRECT,
            location = MatchAfterImmediately(),
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "SettingAppAdminDataSourceImpl.kt" },
) {
    val versionTextConstructor get() = instructionMatches.last().methodReference
}

internal object AdaptiveVersionInfoPreviewFingerprint : Fingerprint(
    name = "emit",
    parameters = listOf("Ljava/lang/Object;", "Lkotlin/coroutines/Continuation;"),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        string({ packageMetadata.versionName }),
        fieldAccess(name = "title_for_need_to_update", opcode = Opcode.SGET),
        methodCall(reference = { AdaptiveVersionInfoFingerprint.versionTextConstructor }),
    ),
    custom = { _, classDef -> classDef.sourceFile == "Emitters.kt" },
)
