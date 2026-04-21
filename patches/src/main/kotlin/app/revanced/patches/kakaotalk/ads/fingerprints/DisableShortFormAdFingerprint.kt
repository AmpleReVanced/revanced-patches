package app.revanced.patches.kakaotalk.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import app.revanced.util.hasMethodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ShortFormAdLoadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("feedAdLoader"),
    custom = { method, classDef ->
        classDef.sourceFile == "SftAdFragment.kt" &&
                method.parameterTypes.isEmpty() &&
                method.hasMethodCall(
                    "Lcom/kakao/adfit/ads/feed/FeedAdLoader;",
                    "isLoading",
                    "Z"
                )
    }
)

internal object ShortFormAdRenderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("feedAdLayout"),
    custom = { method, classDef ->
        classDef.sourceFile == "SftAdFragment.kt" &&
                method.parameterTypes.size == 2 &&
                method.hasMethodCall(
                    "Lcom/kakao/adfit/ads/feed/FeedAdLayout;",
                    "setFeedAd",
                    "V"
                )
    }
)
