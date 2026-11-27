package app.revanced.patches.kakaotalk.misc.tracker

import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.replaceStrings
import app.morphe.patcher.string
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.BuildSdkTrackerUrlFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableSdkTrackerPatch = patch(
    name = "Disable SDK Tracker",
    description = "Disables the SDK Tracker in KakaoTalk."
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        replaceStrings(string("ad.daum.net", StringComparisonType.CONTAINS) to "example.com")
        BuildSdkTrackerUrlFingerprint.method.returnEarly("")
    }
}
