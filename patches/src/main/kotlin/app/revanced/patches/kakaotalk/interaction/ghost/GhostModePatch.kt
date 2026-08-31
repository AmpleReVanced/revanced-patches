package app.revanced.patches.kakaotalk.interaction.ghost

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/GhostModePatch;"

@Suppress("unused")
val ghostMode = bytecodePatch(
    name = "Ghost Mode",
    description = "Don't expose your typing status to the other party.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_ghost_mode",
                titleKey = "morphe_settings_patch_ghost_mode",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val locoMethodClass = LocoMethodClassFingerprint.classDef
        val actionJobClass = actionJobMethodFingerprint(locoMethodClass).classDef
        val sendActionMethod = sendCurrentActionFingerprint(actionJobClass).method
        val protocolSuccessClass = ProtocolSuccessFingerprint.classDef
        val registers = sendActionMethod.getFreeRegisterProvider(0, 2)
        val valueRegister = registers.getFreeRegister4Bit()
        val resultRegister = registers.getFreeRegister4Bit()

        sendActionMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->enableGhostMode()Z
                move-result v$valueRegister
                if-eqz v$valueRegister, :morphe_original
                const/4 v$valueRegister, 0x0
                new-instance v$resultRegister, ${protocolSuccessClass.type}
                invoke-direct {v$resultRegister, v$valueRegister}, ${protocolSuccessClass.type}-><init>(Ljava/lang/Object;)V
                return-object v$resultRegister
                :morphe_original
                nop
            """
        )
    }
}
