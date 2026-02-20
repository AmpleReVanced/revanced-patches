package app.revanced.patches.kakaotalk.tab

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.tab.fingerprints.isChatListCollapseButtonEnabledFingerprint

@Suppress("unused")
val disableCollapseButtonPatch = bytecodePatch(
    name = "Disable Collapse Button",
    description = "Disable collapse button on OpenChatList",
) {
    compatibleWith("com.kakao.talk"("26.1.3"))

    execute {
        isChatListCollapseButtonEnabledFingerprint.method.apply {
            addInstructions(
                0,
                """
                    const/4 p0, 0x0
                    return p0
                """.trimIndent()
            )
        }
    }
}