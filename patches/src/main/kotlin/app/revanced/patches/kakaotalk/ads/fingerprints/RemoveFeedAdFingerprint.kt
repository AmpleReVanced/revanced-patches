package app.revanced.patches.kakaotalk.ads.fingerprints

import app.revanced.patcher.fingerprint

internal val feedAdLayoutFingerprint = fingerprint {
    parameters("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I")
    returns("V")
    custom { method, classDef -> classDef.type == "Lcom/kakao/adfit/ads/feed/FeedAdLayout;" && method.name == "<init>" }
}