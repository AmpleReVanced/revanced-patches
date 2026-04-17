package app.revanced.patches.kakaotalk.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PayBannerLoadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.XOR_INT_LIT8,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "PayAdPlatformBannerState.kt" &&
                method.parameterTypes.size == 1
    }
)

internal object PayBannerRenderFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("productId"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_FROM16,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "PayAdPlatformAdFit.kt"
    }
)
