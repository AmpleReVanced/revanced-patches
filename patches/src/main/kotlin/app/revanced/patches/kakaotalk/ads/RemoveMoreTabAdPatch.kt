package app.revanced.patches.kakaotalk.ads

import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ads.fingerprints.adBigUIModelFingerprint
import app.revanced.patches.kakaotalk.ads.fingerprints.addSectionToMoreTabUIFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val removeMoreTabAdPatch = bytecodePatch(
    name = "Remove More tab ad",
    description = "Removes the ad from the More tab.",
) {
    compatibleWith("com.kakao.talk"("25.7.3"))

    execute {
        val addSectionToMoreTabUIMethod = addSectionToMoreTabUIFingerprint.method
        val addSectionToMoreTabUIInsns = addSectionToMoreTabUIMethod.instructions

        val adBigUIModelClass = adBigUIModelFingerprint.method.definingClass

        val matches = addSectionToMoreTabUIInsns.mapIndexedNotNull { idx, inst ->
            if (inst is BuilderInstruction35c
                && inst.opcode == Opcode.INVOKE_VIRTUAL
                && (inst.getReference<MethodReference>()?.name == "add")
            ) {
                val prev = addSectionToMoreTabUIInsns.getOrNull(idx - 1) as? BuilderInstruction35c
                val ref = (prev?.getReference<MethodReference>())
                if (ref?.definingClass == adBigUIModelClass) {
                    Pair(idx - 1, idx)
                } else null
            } else null
        }

        matches
            .sortedByDescending { it.second }
            .forEach { (loadIdx, invokeIdx) ->
                addSectionToMoreTabUIMethod.removeInstruction(invokeIdx)
                addSectionToMoreTabUIMethod.removeInstruction(loadIdx)
            }
    }
}