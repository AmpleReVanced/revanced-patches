package app.revanced.patches.kakaotalk.misc.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object UtilityGetSignatureFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Ljava/lang/String;",
    strings = listOf("SHA"),
    filters = listOf(
        methodCall("Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;"),
        methodCall("Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;"),
        methodCall("Landroid/util/Base64;->encodeToString([BI)Ljava/lang/String;"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "Utility.kt" }
)

internal object PlatformSignatureFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        methodCall("Landroid/content/Context;->getPackageName()Ljava/lang/String;"),
        methodCall(
            definingClass = "this",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
            returnType = "Ljava/lang/String;",
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "PlatformUtils.kt" },
)