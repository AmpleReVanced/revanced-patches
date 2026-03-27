package app.revanced.patches.kakaotalk.tracker

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.tracker.fingerprints.talkShareServiceInit
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val disableTalkShareLogPatch = bytecodePatch(
    name = "Disable Talk Share Log",
    description = "Disable talk share log"
) {
    compatibleWith("com.kakao.talk"("26.2.2"))

    execute {
        talkShareServiceInit.method.apply {
            addInstructions(
                instructions.first { it.opcode == Opcode.SGET_OBJECT && it.getReference<FieldReference>()?.type == "Ljava/lang/String;" }.location.index + 1,
                "const-string v0, \"example.com\""
            )
        }
    }
}