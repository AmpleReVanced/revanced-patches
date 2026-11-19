package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.findFieldFromToString
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.ChatPaneStateFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.chatDetailNavigationFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.chatListPaneFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/DisableNavigationBarAnimationPatch;"

@Suppress("unused")
val disableNavigationBarAnimationPatch = patch(
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

        completeChatEntryWithoutAnimation(chatDetailNavigationFingerprint())

        chatListPaneFingerprint().apply {
            val index = instructionMatches.single().index
            val scaffold = method.getInstruction<FiveRegisterInstruction>(index)
            val registers = method.getFreeRegisterProvider(index, 3)
            val twoPane = registers.getFreeRegister4Bit()
            val detail = registers.getFreeRegister4Bit()
            val settled = registers.getFreeRegister4Bit()
            val paneState = ChatPaneStateFingerprint.originalMethod
            method.addInstructionsWithLabels(
                index,
                """
                    iget-boolean v$twoPane, p0, ${paneState.findFieldFromToString(", isTwoPaneLayout=")}
                    iget-boolean v$detail, p0, ${paneState.findFieldFromToString(", isDetailPaneActive=")}
                    iget-boolean v$settled, p0, ${paneState.findFieldFromToString(", isListPaneSettledVisible=")}
                    invoke-static {v$twoPane, v$detail, v$settled, v${scaffold.registerD}, v${scaffold.registerE}}, $EXTENSION_CLASS->renderListPreview(ZZZLkotlin/jvm/functions/Function2;Ljava/lang/Object;)Z
                    move-result v$detail
                    if-nez v$detail, :after_bar
                """,
                ExternalLabel("after_bar", method.getInstruction(index + 1)),
            )
        }
    }
}
