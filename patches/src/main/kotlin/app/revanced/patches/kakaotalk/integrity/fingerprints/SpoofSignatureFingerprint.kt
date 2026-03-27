package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object UtilityGetSignatureFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Ljava/lang/String;",
    strings = listOf("context", "SHA", "encodeToString(md.digest(), Base64.NO_WRAP)"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    custom = { _, classDef -> classDef.sourceFile == "Utility.kt" }
)