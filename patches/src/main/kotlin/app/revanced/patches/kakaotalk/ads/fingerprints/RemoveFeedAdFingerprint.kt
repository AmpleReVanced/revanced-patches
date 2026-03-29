package app.revanced.patches.kakaotalk.ads.fingerprints

import app.morphe.patcher.Fingerprint

internal object FeedAdLayoutFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/adfit/ads/feed/FeedAdLayout;",
    name = "<init>",
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I"),
    returnType = "V",
)