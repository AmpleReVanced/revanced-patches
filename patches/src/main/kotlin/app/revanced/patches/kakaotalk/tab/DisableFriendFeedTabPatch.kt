package app.revanced.patches.kakaotalk.tab

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.tab.fingerprints.mainTabConfigFingerprint

@Suppress("unused")
val disableFriendFeedTabPatch = bytecodePatch(
    name = "Disable Friend Feed tab",
    description = "Disables the Friend Feed tab in KakaoTalk.",
) {
    compatibleWith("com.kakao.talk"("25.11.0"))

    execute {
        mainTabConfigFingerprint.method.addInstructions(
            mainTabConfigFingerprint.method.instructions.size - 1,
            """
                const/4 p1, 0x0
                iput-boolean p1, p0, ${mainTabConfigFingerprint.method.definingClass}->a:Z
            """.trimIndent()
        )
    }
}