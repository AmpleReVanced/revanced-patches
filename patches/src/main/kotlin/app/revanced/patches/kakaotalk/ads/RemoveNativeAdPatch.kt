package app.revanced.patches.kakaotalk.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ads.fingerprints.loadNativeAdFingerprint

@Suppress("unused")
val removeNativeAdPatch = bytecodePatch(
    name = "Remove native ad",
    description = "Removes the native ad from the app.",
) {
    compatibleWith("com.kakao.talk"("25.8.2"))

    execute {
        val method = loadNativeAdFingerprint.method

        method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )
    }
}