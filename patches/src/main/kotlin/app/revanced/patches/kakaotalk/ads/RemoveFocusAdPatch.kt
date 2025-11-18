package app.revanced.patches.kakaotalk.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ads.fingerprints.loadFocusAdFingerprint

@Suppress("unused")
val removeFocusAdPatch = bytecodePatch(
    name = "Remove focus ad",
    description = "Removes the focus ad from the app.",
) {
    compatibleWith("com.kakao.talk"("25.10.0"))

    execute {
        loadFocusAdFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )
    }
}