package app.revanced.patches.kakaotalk.misc

import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.misc.fingerprints.addNavigationTabFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val removeShopTabPatch = bytecodePatch(
    name = "Remove shop tab",
    description = "Removes the shop tab from the bottom navigation bar.",
) {
    compatibleWith("com.kakao.talk"("25.7.3"))

    execute {
        val method = addNavigationTabFingerprint.method
        val insns  = method.instructions

        val matches = insns.mapIndexedNotNull { idx, inst ->
            if (inst is BuilderInstruction35c
                && inst.opcode == Opcode.INVOKE_VIRTUAL
                && (inst.getReference<MethodReference>()?.name == "add")
            ) {
                val prev = insns.getOrNull(idx - 1) as? BuilderInstruction21c
                val fldName = (prev?.reference as? FieldReference)?.name
                if (fldName == "SHOPPING_TAB" || fldName == "CALL_TAB") {
                    Pair(idx - 1, idx)
                } else null
            } else null
        }

        matches
            .sortedByDescending { it.second }
            .forEach { (loadIdx, invokeIdx) ->
                method.removeInstruction(invokeIdx)
                method.removeInstruction(loadIdx)
            }
    }
}