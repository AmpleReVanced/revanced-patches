package app.revanced.patches.kakaotalk.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object TailBannerRenderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L"),
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Lcom/kakao/talk/widget/tab/banner/BannerViewHolder;",
            name = "bind",
            returnType = "V",
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "TailBannerAdapter.kt" },
)

internal object TalkBannerLoadFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/adfit/ads/talk/TalkBannerAdView;",
    name = "loadAd",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "V",
)