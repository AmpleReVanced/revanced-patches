package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object VerifyingSignatureFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("getPackageName(...)"),
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