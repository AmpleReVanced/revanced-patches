package app.revanced.patches.kakaotalk.ads

import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ads.fingerprints.adBigUIModelFingerprint
import app.revanced.patches.kakaotalk.ads.fingerprints.addSectionToMoreTabUIFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val removeMoreTabAdPatch = bytecodePatch(
    name = "Remove More tab ad",
    description = "Removes the ad from the More tab.",
) {
    compatibleWith("com.kakao.talk"("25.8.1"))

    execute {
        val method = addSectionToMoreTabUIFingerprint.method
        val insns = method.instructions

        val adBigWrapperClass = adBigUIModelFingerprint.classDef.toString()

        val removalGroups = mutableListOf<List<Int>>()

        // Scan for patterns
        var i = 0
        while (i < insns.size - 2) {
            val inst0 = insns[i]
            val inst1 = insns[i + 1]
            val inst2 = insns[i + 2]

            // find pattern: new-instance, invoke-direct <init>, invoke-interface List.add
            // 1) new-instance
            val isNewInstance = inst0.opcode == Opcode.NEW_INSTANCE &&
                    (inst0 as BuilderInstruction21c).reference.toString() == adBigWrapperClass

            // 2) call <init> (invoke-direct)
            val isCtor = inst1 is BuilderInstruction35c &&
                    inst1.opcode == Opcode.INVOKE_DIRECT &&
                    inst1.getReference<MethodReference>()?.let { m ->
                        m.definingClass == adBigWrapperClass && m.name == "<init>"
                    } == true

            // 3) call List.add (invoke-interface)
            val isAdd = inst2 is BuilderInstruction35c &&
                    inst2.opcode == Opcode.INVOKE_INTERFACE &&
                    inst2.getReference<MethodReference>()?.let { m ->
                        m.definingClass == "Ljava/util/List;" && m.name == "add"
                    } == true

            if (isNewInstance && isCtor && isAdd) {
                removalGroups += listOf(i, i + 1, i + 2)
                // Skip ahead to avoid overlapping matches
                i += 3
            } else {
                i++
            }
        }

        // delete in reverse order to maintain indices
        removalGroups.flatten().distinct().sortedDescending().forEach { idx ->
            method.removeInstruction(idx)
        }
    }
}