package app.revanced.patches.shared.misc.string

import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.string
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

fun replaceStringPatch(
    from: String,
    to: String
) = patch(
    description = "Replaces occurrences of '$from' with '$to' in string references.",
) {
    execute {
        string(from, StringComparisonType.CONTAINS).matchAllMethodIndicesForEach(requireMatches = false) { index ->
            val instruction = getInstruction<OneRegisterInstruction>(index)
            val string = getInstruction(index).getReference<StringReference>()!!.string.replace(from, to)
            replaceInstruction(index, "${instruction.opcode.name} v${instruction.registerA}, \"$string\"")
        }
    }
}
