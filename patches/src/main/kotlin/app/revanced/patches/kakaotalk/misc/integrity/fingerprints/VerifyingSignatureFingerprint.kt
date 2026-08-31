package app.revanced.patches.kakaotalk.misc.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object VerifyingSignatureFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        methodCall("Landroid/content/Context;->getPackageName()Ljava/lang/String;"),
        methodCall(
            definingClass = "this",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
            returnType = "Ljava/lang/String;",
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "PlatformUtils.kt" },
)

internal object IntentResolveClientMethod : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/pm/PackageInfo;"),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.GOTO,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.IF_NEZ
    ),
    custom = { _, classDef -> classDef.sourceFile == "IntentResolveClient.kt" }
)