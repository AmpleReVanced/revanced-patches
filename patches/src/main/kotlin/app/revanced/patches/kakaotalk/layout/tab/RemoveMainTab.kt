package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.smali.ExternalLabel
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.mainTabFingerprint
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val SETTINGS_CLASS = "Lapp/revanced/extension/kakaotalk/settings/Settings;"

internal fun BytecodePatchContext.removeMainTab(tabName: String, settingMethod: String) {
    mainTabFingerprint(tabName).apply {
        val tabIndex = instructionMatches.first().index
        val register = method.getInstruction<OneRegisterInstruction>(tabIndex).registerA
        method.addInstructionsWithLabels(
            tabIndex,
            """
                invoke-static {}, $SETTINGS_CLASS->$settingMethod()Z
                move-result v$register
                if-eqz v$register, :show_tab
                const/4 v$register, 0x0
                return-object v$register
            """,
            ExternalLabel("show_tab", method.getInstruction(tabIndex)),
        )
    }
}
