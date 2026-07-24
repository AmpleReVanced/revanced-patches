package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.Fingerprint

internal object ListBannerAdRequestFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    strings = listOf("LIST_BANNER_MOBILE_ANDROID_APP"),
)

internal object LiveDownBannerRequestFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    strings = listOf("LIVE_DOWN_ANDROID_APP"),
)