package app.revanced.patches.kakaotalk.ad.fingerprints

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
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf(
        "Ljava/lang/String;",
        "Lcom/kakao/adfit/ads/media/NativeAdBinder;",
        "L",
        "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function0;",
        "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function0;",
        "L",
        "I",
        "I",
    ),
    returnType = "V",
    custom = { _, classDef -> classDef.sourceFile == "PayAdPlatformAdFit.kt" }
)
