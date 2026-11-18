package app.revanced.patches.kakaotalk.misc.tracker

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.BuildSdkTrackerUrlFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.shared.misc.string.replaceStringPatch

@Suppress("unused")
val disableSdkTrackerPatch = patch(
    name = "Disable SDK Tracker",
    description = "Disables the SDK Tracker in KakaoTalk."
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(
        replaceStringPatch("ad.daum.net", "example.com")
    )

    execute {
        BuildSdkTrackerUrlFingerprint.method.returnEarly("")
    }
}
