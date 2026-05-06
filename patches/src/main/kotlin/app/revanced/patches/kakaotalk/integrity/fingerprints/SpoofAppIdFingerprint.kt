package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object GetAppIdFingerprint : Fingerprint(
    name = "GetAppID",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Ljava/lang/String;",
    strings = listOf("@\u0015s1w\u0000N4", "??-98", "OnePassManager"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    custom = { _, classDef -> classDef.sourceFile == "OnePassManager.java" }
)

internal object UaffacetidMethodFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;"),
    returnType = "Ljava/lang/String;",
    custom = { _, classDef -> classDef.sourceFile == "UAFFacetID.java" }
)