package app.revanced.patches.kakaotalk.chatlog

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patches.kakaotalk.chatlog.fingerprints.checkIsAllowedHideFingerprint

@Suppress("unused")
val allowHideOnAnyChatPatch = app.revanced.patcher.patch.bytecodePatch(
    name = "Allow Hide on Any Chat",
    description = "Users with hiding privileges can hide any chat, including their own messages.",
) {
    compatibleWith("com.kakao.talk"("25.7.3"))

    execute {
        checkIsAllowedHideFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )
    }
}