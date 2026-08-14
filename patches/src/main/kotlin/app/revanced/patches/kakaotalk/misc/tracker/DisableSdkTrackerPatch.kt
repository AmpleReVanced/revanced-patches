package app.revanced.patches.kakaotalk.misc.tracker

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.BuildSdkTrackerUrlFingerprint
import app.revanced.patches.shared.misc.string.replaceStringPatch

@Suppress("unused")
val disableSdkTrackerPatch = bytecodePatch(
    name = "Disable SDK Tracker",
    description = "Disables the SDK Tracker in KakaoTalk."
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(
        replaceStringPatch("ad.daum.net", "example.com")
    )

    execute {
        val method = BuildSdkTrackerUrlFingerprint.method
        val register = method.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        method.addInstructions(
            0,
            """
                const-string v$register, ""
                return-object v$register
            """.trimIndent()
        )
    }
}
