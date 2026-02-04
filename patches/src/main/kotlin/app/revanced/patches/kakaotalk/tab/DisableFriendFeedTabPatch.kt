package app.revanced.patches.kakaotalk.tab

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.tab.fingerprints.determineFeedOrListMethodFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.isHideFriendsTabSettingsFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.mainTabConfigFingerprint
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val disableFriendFeedTabPatch = bytecodePatch(
    name = "Disable Friend Feed tab",
    description = "Disables the Friend Feed tab in KakaoTalk.",
) {
    compatibleWith("com.kakao.talk"("26.1.2"))

    execute {
        mainTabConfigFingerprint.method.addInstructions(
            mainTabConfigFingerprint.method.instructions.size - 1,
            """
                const/4 p1, 0x0
                iput-boolean p1, p0, ${mainTabConfigFingerprint.method.definingClass}->a:Z
            """.trimIndent()
        )

        isHideFriendsTabSettingsFingerprint.method.addInstructions(
            0,
            """
                const/4 v1, 0x0
                return v1
            """.trimIndent()
        )

        determineFeedOrListMethodFingerprint.method.apply {
            val instIndex = instructions.indexOfLast { it.opcode == Opcode.IF_EQZ }
            addInstruction(
                instIndex,
                "const/4 p1, 0x0"
            )
        }
    }
}