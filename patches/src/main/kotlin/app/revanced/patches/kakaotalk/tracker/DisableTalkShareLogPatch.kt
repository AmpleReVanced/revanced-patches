package app.revanced.patches.kakaotalk.tracker

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.tracker.fingerprints.talkShareServiceInit
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val disableTalkShareLogPatch = bytecodePatch(
    name = "Disable Talk Share Log",
    description = "Disable talk share log"
) {
    compatibleWith("com.kakao.talk"("26.2.1"))

    execute {
        talkShareServiceInit.method.apply {
            addInstructions(
                instructions.first { it.opcode == Opcode.SGET_OBJECT && it.getReference<FieldReference>()?.type == "Ljava/lang/String;" }.location.index + 1,
                "const-string v0, \"example.com\""
            )
        }
    }
}