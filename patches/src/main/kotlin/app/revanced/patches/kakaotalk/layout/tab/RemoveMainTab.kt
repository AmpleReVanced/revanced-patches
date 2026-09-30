package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.mainTabFingerprint
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal fun BytecodePatchContext.removeMainTab(tabName: String) {
    mainTabFingerprint(tabName).apply {
        val tab = instructionMatches.first()
        val register = tab.getInstruction<OneRegisterInstruction>().registerA
        method.replaceInstruction(tab.index, "const/4 v$register, 0x0")
    }
}
