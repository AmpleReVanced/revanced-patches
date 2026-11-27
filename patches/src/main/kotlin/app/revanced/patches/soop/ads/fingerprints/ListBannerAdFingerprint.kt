package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

private const val LIST_BANNER_PLACEMENT = "LIST_BANNER_MOBILE_ANDROID_APP"

internal object ListBannerAdRequestFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    filters = listOf(string(LIST_BANNER_PLACEMENT)) + samAdResponseFilters(),
)

internal object LiveDownBannerRequestFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    strings = listOf("LIVE_DOWN_ANDROID_APP"),
)