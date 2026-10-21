package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabPagerGameItemFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabPagerHomeItemFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.MoreTabPagerSelectionFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.moreTabPagerItemIdFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.moreTabPagerItemMapperFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.moreTabPagerItemsFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/HideMoreTabGamePatch;"
private const val HIDE_GAME = "Lapp/revanced/extension/kakaotalk/settings/Settings;->hideMoreTabGame()Z"

@Suppress("unused")
val hideMoreTabGamePatch = bytecodePatch(
    name = "Hide More tab Game tab",
    description = "Hides the Game tab from the More tab.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.NAVIGATION.addPreferences(
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_game",
                titleKey = "morphe_settings_catalog_hide_more_tab_game",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val gameClass = MoreTabPagerGameItemFingerprint.originalClassDef
        val homeClass = MoreTabPagerHomeItemFingerprint.originalClassDef
        val gameField = gameClass.staticFields.single { it.type == gameClass.type }.smaliReference
        val homeField = homeClass.staticFields.single { it.type == homeClass.type }.smaliReference
        val pagerType = gameClass.superclass
            ?: throw PatchException("Could not infer MoreTabPagerItem super type")
        val itemId = moreTabPagerItemIdFingerprint(pagerType).originalMethod.smaliReference

        moreTabPagerItemsFingerprint(gameClass.type).apply {
            val gameFieldIndex = instructionMatches[0].index
            val arrayIndex = instructionMatches[1].index
            val gameFieldRegister = method.getInstruction<OneRegisterInstruction>(gameFieldIndex).registerA
            val gameItemRegister = (gameFieldIndex + 1 until arrayIndex).firstNotNullOfOrNull { index ->
                (method.getInstruction(index) as? TwoRegisterInstruction)?.takeIf {
                    it.opcode == Opcode.MOVE_OBJECT && it.registerB == gameFieldRegister
                }?.registerA
            } ?: throw PatchException("Could not find Game tab item register")
            val register = method.getFreeRegisterProvider(arrayIndex, 1, gameItemRegister).getFreeRegister4Bit()
            method.addInstructionsWithLabels(
                arrayIndex,
                """
                    invoke-static {}, $HIDE_GAME
                    move-result v$register
                    if-eqz v$register, :keep_game
                    const/4 v$gameItemRegister, 0x0
                    :keep_game
                    nop
                """,
            )
        }

        moreTabPagerItemMapperFingerprint(gameClass.type, pagerType).apply {
            val returnIndex = instructionMatches[1].index
            val itemRegister = method.getInstruction<OneRegisterInstruction>(returnIndex).registerA
            val register = method.getFreeRegisterProvider(returnIndex, 1, itemRegister).getFreeRegister4Bit()
            method.addInstructionsWithLabels(
                returnIndex,
                """
                    invoke-static {}, $HIDE_GAME
                    move-result v$register
                    if-eqz v$register, :keep_game_item
                    const/4 v$itemRegister, 0x0
                    :keep_game_item
                    nop
                """,
            )
        }

        MoreTabPagerSelectionFingerprint.method.apply {
            val registers = getFreeRegisterProvider(0, 2)
            val itemRegister = registers.getFreeRegister4Bit()
            val flagRegister = registers.getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $HIDE_GAME
                    move-result v$flagRegister
                    if-eqz v$flagRegister, :keep_selection
                    sget-object v$itemRegister, $gameField
                    invoke-virtual {v$itemRegister}, $itemId
                    move-result-object v$itemRegister
                    invoke-virtual {v$itemRegister, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v$flagRegister
                    if-eqz v$flagRegister, :keep_selection
                    sget-object v$itemRegister, $homeField
                    invoke-virtual {v$itemRegister}, $itemId
                    move-result-object p1
                    :keep_selection
                    nop
                """,
            )
        }
    }
}
