package app.revanced.patches.kakaotalk.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ads.fingerprints.checkDisableFriendListsAdFingerprint

@Suppress("unused")
val disableFriendListsAdPatch = bytecodePatch(
    name = "Disable Friend Lists ad",
    description = "Disables the Friend Lists ad in KakaoTalk.",
) {
    compatibleWith("com.kakao.talk"("25.11.2"))

    execute {
        val checkDisableFriendListsAdMethod = checkDisableFriendListsAdFingerprint.method

        checkDisableFriendListsAdMethod.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )
    }
}