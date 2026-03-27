package app.revanced.patches.kakaotalk.tracker

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tracker.fingerprints.TalkShareServiceInit
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val disableTalkShareLogPatch = bytecodePatch(
    name = "Disable Talk Share Log",
    description = "Disable talk share log"
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        TalkShareServiceInit.method.apply {
            addInstructions(
                instructions.first { it.opcode == Opcode.SGET_OBJECT && it.getReference<FieldReference>()?.type == "Ljava/lang/String;" }.location.index + 1,
                "const-string v0, \"example.com\""
            )
        }
    }
}