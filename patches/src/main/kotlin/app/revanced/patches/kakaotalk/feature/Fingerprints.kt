package app.revanced.patches.kakaotalk.feature

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object GetFeatureFlagValueFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    strings = listOf("type"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.SGET_OBJECT,
        Opcode.SGET_BOOLEAN,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.RETURN
    ),
    custom = { _, classDef -> classDef.sourceFile == "Feature.kt" }
)

internal object GetFeatureFlagsInExtensionFingerprint : Fingerprint(
    definingClass = "Lapp/revanced/extension/kakaotalk/feature/Flag;",
    name = "getFeatureFlags"
)