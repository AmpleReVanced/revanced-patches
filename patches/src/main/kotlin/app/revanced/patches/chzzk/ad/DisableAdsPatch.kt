package app.revanced.patches.chzzk.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.returnEarly
import app.revanced.patches.chzzk.shared.Constants.COMPATIBILITY_CHZZK
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Suppress("unused")
val disableAdsPatch = bytecodePatch(
    name = "Disable ads",
    description = "Disables CHZZK advertisements, including live stream pre-roll, mid-roll and " +
        "post-roll ads and clip feed ads.",
) {
    compatibleWith(COMPATIBILITY_CHZZK)

    execute {
        ApplyPlayerAdParamsFingerprint.method.addInstructions(0, "return-object p0")
        AdEnterPlayerFingerprint.method.returnEarly()
        MapRecommendedCardsFingerprint.method.apply {
            val adTypeIndex = indexOfFirstInstructionOrThrow {
                getReference<StringReference>()?.string == "AD"
            }
            val adListAddIndex = indexOfFirstInstructionOrThrow(adTypeIndex + 1) {
                getReference<MethodReference>()?.let { reference ->
                    reference.definingClass == "Ljava/util/ArrayList;" &&
                        reference.name == "add" &&
                        reference.parameterTypes == listOf("Ljava/lang/Object;")
                } == true
            }
            replaceInstruction(adListAddIndex, "nop")
        }
    }
}