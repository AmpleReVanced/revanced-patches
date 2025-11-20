package app.revanced.patches.kakaotalk.tracker

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.tracker.fingerprints.buildSdkTrackerUrlFingerprint
import app.revanced.patches.shared.misc.string.replaceStringPatch

@Suppress("unused")
val disableSdkTrackerPatch = bytecodePatch(
    name = "Disable SDK Tracker",
    description = "Disables the SDK Tracker in KakaoTalk."
) {
    compatibleWith("com.kakao.talk"("25.10.1"))
    dependsOn(
        replaceStringPatch("ad.daum.net", "example.com")
    )

    execute {
        buildSdkTrackerUrlFingerprint.method.addInstructions(
            0,
            """
                const-string p0, ""
                return-object p0
            """.trimIndent()
        )
    }
}