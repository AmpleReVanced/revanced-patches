package app.revanced.patches.flexcil.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object IsCanUsePremiumFeaturesFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf(),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.GOTO_16,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    ),
    strings = listOf("productIdentifiers"),
    custom = { method, classDef -> classDef.sourceFile == "FlexcilProducts.kt" }
)

internal object IsPurchaseSpecificProductFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Ljava/util/List;"),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
        Opcode.SGET_OBJECT
    ),
    strings = listOf("productIdentifiers", "toLowerCase(...)", "Don\'t cracking. It\'s Illegal."),
    custom = { method, classDef -> classDef.sourceFile == "ProductDetailsHelper.kt" }
)

internal object IsDownloadablePremiumContent : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf(),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.CONST_STRING,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.SGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    strings = listOf("productIdentifiers"),
    custom = { method, classDef -> classDef.sourceFile == "FlexcilAccountClient.kt" }
)