package app.revanced.patches.kakaotalk.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object OpenChatFeedAdapterFingerprint : Fingerprint(
    name = "getItemViewType",
    parameters = listOf("I"),
    returnType = "I",
    custom = { _, classDef -> classDef.sourceFile == "OpenChatTabFeedAdapter.kt" },
)

internal object OpenChatFeedAdItemFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("OpenChatTabFeedAdUiModel(feedOrder="),
    custom = { _, classDef -> classDef.sourceFile == "OpenChatFeedBaseUiModel.kt" },
)

internal object OpenChatFeedAdLoadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(methodCall(definingClass = "Lcom/kakao/adfit/ads/feed/FeedAdLoader;", name = "loadAd", returnType = "Z")),
    custom = { method, classDef ->
        classDef.sourceFile == "OpenChatTabFeedAdViewHolder.kt" && method.parameterTypes.size == 2
    }
)

internal object OpenChatFeedAdRenderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(methodCall(definingClass = "Lcom/kakao/adfit/ads/feed/FeedAdLayout;", name = "setFeedAd", returnType = "V")),
    custom = { method, classDef ->
        classDef.sourceFile == "OpenChatTabFeedAdViewHolder.kt" && method.parameterTypes.size == 3
    }
)
