package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.ad.fingerprints.AdBigUIModelFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.AddMoreTabServiceSectionsFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val removeMoreTabAdPatch = bytecodePatch(
    name = "Remove More tab ad",
    description = "Removes the ad from the More tab.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val adType = AdBigUIModelFingerprint.classDef.type
        val method = AddMoreTabServiceSectionsFingerprint.method
        val additions = method.findInstructionIndicesReversedOrThrow(
            methodCall("Ljava/util/List;->add(Ljava/lang/Object;)Z"),
        ).filter { index ->
            method.getInstruction(index - 1).getReference<MethodReference>()?.let {
                it.definingClass == adType && it.name == "<init>"
            } == true
        }
        if (additions.size != 2) throw PatchException("Could not resolve More tab ad additions.")
        additions.forEach { method.replaceInstruction(it, "nop") }
    }
}