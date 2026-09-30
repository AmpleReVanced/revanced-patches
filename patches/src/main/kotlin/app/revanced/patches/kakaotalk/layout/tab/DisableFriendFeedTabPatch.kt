package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.DetermineFeedOrListMethodFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MainTabConfigFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/DisableFriendFeedTabPatch;"

@Suppress("unused")
val disableFriendFeedTabPatch = bytecodePatch(
    name = "Disable Friend Feed tab",
    description = "Adds an option to replace the Friend Feed tab with the classic Friends tab.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.NAVIGATION.addPreferences(
            SwitchPreference(
                key = "morphe_pref_disable_friend_feed_tab",
                titleKey = "morphe_settings_catalog_disable_friend_feed_tab",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        MainTabConfigFingerprint.apply {
            val feedTabIndex = instructionMatches[1].index
            val register = method.getInstruction<TwoRegisterInstruction>(feedTabIndex).registerA
            method.addInstructions(
                feedTabIndex,
                """
                    invoke-static {v$register}, $EXTENSION_CLASS->isFriendFeedEnabled(Z)Z
                    move-result v$register
                """,
            )
        }

        DetermineFeedOrListMethodFingerprint.instructionMatches.first().getMethodCalled().apply {
            val returnIndex = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN)
            val register = getInstruction<OneRegisterInstruction>(returnIndex).registerA
            addInstructions(
                returnIndex,
                """
                    invoke-static {v$register}, $EXTENSION_CLASS->isFriendFeedEnabled(Z)Z
                    move-result v$register
                """,
            )
        }
    }
}
