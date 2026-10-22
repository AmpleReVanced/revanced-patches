package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.ChatNavigationProgressFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MainNavigationSuiteFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.chatDetailNavigationFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.chatListPaneVisibilityFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.chatNavigationVisibilityFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/DisableNavigationBarAnimationPatch;"

@Suppress("unused")
val disableNavigationBarAnimationPatch = bytecodePatch(
    name = "Disable navigation bar animation",
    description = "Adds an option to enter chats without transition delays and show the bottom bar immediately on return.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.NAVIGATION.addPreferences(
            SwitchPreference(
                key = "morphe_pref_disable_navigation_bar_animation",
                titleKey = "morphe_settings_catalog_disable_navigation_bar_animation",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        MainNavigationSuiteFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range {p9 .. p9}, $EXTENSION_CLASS->shouldAnimateBarReturn(Z)Z
                move-result p9
            """,
        )

        val isBackTransitionRunning = ChatNavigationProgressFingerprint.instructionMatches[1].getMethodCalled()
        val isListPaneHidden = chatListPaneVisibilityFingerprint().instructionMatches.single().getMethodCalled()
        chatNavigationVisibilityFingerprint().apply {
            val backStateRegister = method.getInstruction<FiveRegisterInstruction>(instructionMatches[0].index).registerE
            val paneStateRegister = method.getInstruction<RegisterRangeInstruction>(instructionMatches[1].index).startRegister
            val visibilityIndex = instructionMatches[2].index
            val visibilityRegister = method.getInstruction<FiveRegisterInstruction>(visibilityIndex).registerC
            val hiddenRegister = method.getInstruction<OneRegisterInstruction>(instructionMatches[3].index).registerA
            val backRegister = method.getInstruction<OneRegisterInstruction>(instructionMatches[4].index).registerA

            method.addInstructions(
                visibilityIndex,
                """
                    invoke-virtual/range {v$paneStateRegister .. v$paneStateRegister}, $isListPaneHidden
                    move-result v$hiddenRegister
                    invoke-static/range {v$backStateRegister .. v$backStateRegister}, $isBackTransitionRunning
                    move-result v$backRegister
                    invoke-static {v$visibilityRegister, v$hiddenRegister, v$backRegister}, $EXTENSION_CLASS->isCompactDetailOpen(ZZZ)Z
                    move-result v$visibilityRegister
                """,
            )
        }

        val entry = chatDetailNavigationFingerprint().also { it.match() }
        completeChatEntryWithoutAnimation(entry)
    }
}
