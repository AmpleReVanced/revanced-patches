package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ads.fingerprints.loadFocusAdFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val removeFocusAdPatch = bytecodePatch(
    name = "Remove focus ad",
    description = "Removes the focus ad from the app.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

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