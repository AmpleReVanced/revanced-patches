package app.revanced.patches.kakaotalk.ad.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object LoadNativeAdFingerprint : Fingerprint(
    name = "load",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("I", "Lcom/kakao/adfit/ads/media/NativeAdLoader\$AdLoadListener;"),
    returnType = "Z",
    strings = listOf(
        " owner is destroyed.",
        " loading is already started.",
        "Request Native AD",
        "Native ad is cached. [id = ",
    ),
)
