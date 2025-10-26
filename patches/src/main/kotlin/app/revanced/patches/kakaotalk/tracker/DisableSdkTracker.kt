package app.revanced.patches.kakaotalk.tracker

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.string.replaceStringPatch

@Suppress("unused")
val disableSdkTrackerPatch = bytecodePatch {
    dependsOn(
        replaceStringPatch("display.ad.daum.net", "example.com")
    )
}