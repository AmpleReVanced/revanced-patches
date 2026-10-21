package app.revanced.patches.kakaotalk.ad.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object TailBannerRenderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf(".MainTailBannerOverlay ("),
    custom = { _, classDef -> classDef.sourceFile == "MainTailBannerOverlay.kt" },
)

internal object TalkBannerLoadFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/adfit/ads/talk/TalkBannerAdView;",
    name = "loadAd",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "V",
)