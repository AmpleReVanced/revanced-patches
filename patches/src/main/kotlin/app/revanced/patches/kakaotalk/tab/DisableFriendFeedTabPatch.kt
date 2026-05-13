package app.revanced.patches.kakaotalk.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tab.fingerprints.DetermineFeedOrListMethodFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.IsHideFriendsTabSettingsFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.MainTabConfigFingerprint
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val disableFriendFeedTabPatch = bytecodePatch(
    name = "Disable Friend Feed tab",
    description = "Disables the Friend Feed tab in KakaoTalk.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        MainTabConfigFingerprint.method.addInstructions(
            MainTabConfigFingerprint.method.instructions.size - 1,
            """
                const/4 p1, 0x0
                iput-boolean p1, p0, ${MainTabConfigFingerprint.method.definingClass}->a:Z
            """.trimIndent()
        )

        IsHideFriendsTabSettingsFingerprint.method.returnEarly(false)

        DetermineFeedOrListMethodFingerprint.method.apply {
            val instIndex = instructions.indexOfLast { it.opcode == Opcode.IF_EQZ }
            addInstruction(
                instIndex,
                "const/4 v1, 0x0"
            )
        }
    }
}