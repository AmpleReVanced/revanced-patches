package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.chatlog.fingerprints.GetWatermarkCountFromCacheFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ProcessWatermarkCountFingerprint
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22t

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/Remove99ClampPatch;"

@Suppress("unused")
val remove99ClampPatch = bytecodePatch(
    name = "Disable 99 unread limit",
    description = "Skip the 99-cap so unread count shows full value"
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_disable_99_unread_limit",
                titleKey = "morphe_settings_patch_disable_99_unread_limit",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        ProcessWatermarkCountFingerprint.method.apply {
            val clamp = instructions
                .filterIsInstance<BuilderInstruction22t>()
                .firstOrNull { it.opcode == Opcode.IF_LE }
                ?: throw PatchException("Could not find IF_LE clamp branch in ChatLogRecyclerItem")
            val clampIndex = instructions.indexOf(clamp)

            val cappedReturnIndex = instructions.withIndex()
                .firstOrNull { (index, instruction) ->
                    index > clampIndex &&
                            instruction.opcode == Opcode.RETURN &&
                            (instruction as? OneRegisterInstruction)?.registerA == clamp.registerB
                }?.index ?: throw PatchException("Could not find capped return in ChatLogRecyclerItem")

            val actualCountRegister = clamp.registerA
            val cappedCountRegister = (getInstruction(cappedReturnIndex) as OneRegisterInstruction).registerA
            val flagRegister = getFreeRegisterProvider(
                cappedReturnIndex,
                1,
                actualCountRegister,
                cappedCountRegister
            ).getFreeRegister()

            removeInstructions(cappedReturnIndex, 1)
            addInstructionsWithLabels(
                cappedReturnIndex,
                """
                    invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->disable99UnreadLimit()Z
                    move-result v$flagRegister
                    if-eqz v$flagRegister, :morphe_return_capped
                    return v$actualCountRegister
                    :morphe_return_capped
                    return v$cappedCountRegister
                """.trimIndent()
            )
        }

        GetWatermarkCountFromCacheFingerprint.method.apply {
            val clamp = instructions
                .filterIsInstance<BuilderInstruction22t>()
                .firstOrNull { it.opcode == Opcode.IF_LE }
                ?: throw PatchException("Could not find IF_LE clamp branch in WatermarksManager")
            val clampIndex = instructions.indexOf(clamp)

            val capMoveIndex = instructions.withIndex()
                .firstOrNull { (index, instruction) ->
                    index > clampIndex &&
                            instruction.opcode == Opcode.MOVE &&
                            (instruction as? TwoRegisterInstruction)?.registerA == clamp.registerA &&
                            instruction.registerB == clamp.registerB
                }?.index ?: throw PatchException("Could not find cap assignment in WatermarksManager")

            val countRegister = clamp.registerA
            val limitRegister = clamp.registerB
            val flagRegister = getFreeRegisterProvider(
                capMoveIndex,
                1,
                countRegister,
                limitRegister
            ).getFreeRegister()

            removeInstructions(capMoveIndex, 1)
            addInstructionsWithLabels(
                capMoveIndex,
                """
                    invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->disable99UnreadLimit()Z
                    move-result v$flagRegister
                    if-nez v$flagRegister, :morphe_skip_cap
                    move v$countRegister, v$limitRegister
                    :morphe_skip_cap
                    nop
                """.trimIndent()
            )
        }
    }
}
