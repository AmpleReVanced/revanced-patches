package app.revanced.patches.kakaotalk.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object ShortFormAdLoadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "V",
    strings = listOf("feedAdLoader"),
    filters = listOf(
        methodCall("Lcom/kakao/adfit/ads/feed/FeedAdLoader;->isLoading()Z"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "SftAdFragment.kt" }
)

internal object ShortFormAdRenderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("feedAdLayout"),
    filters = listOf(
        methodCall("Lcom/kakao/adfit/ads/feed/FeedAdLayout;->setFeedAd(Lcom/kakao/adfit/ads/feed/FeedAd;)V"),
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "SftAdFragment.kt" &&
            method.parameterTypes.size == 2
    }
)
