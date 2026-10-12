package app.revanced.patches.chzzk.homebanner

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.returnEarly
import app.revanced.patches.chzzk.common.utils.forceBooleanGetterFalse
import app.revanced.patches.chzzk.common.utils.forceObjectGetterNull
import app.revanced.patches.chzzk.shared.Constants.COMPATIBILITY_CHZZK
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

@Suppress("unused")
val hideHomeBannerPatch = bytecodePatch(
    name = "Hide home banners",
    description = "Hides promotional banners on the CHZZK home recommend tab, including the top " +
        "banner carousel and the in-feed event and image banners.",
) {
    compatibleWith(COMPATIBILITY_CHZZK)

    execute {
        StreamingHomeBannersFingerprint.classDef.forceObjectGetterNull("banners")
        TopicBannerRowsFingerprint.method.apply {
            listOf("ImageBannerComponent", "SpecialEventBannerComponent").forEach { component ->
                val index = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INSTANCE_OF &&
                        getReference<TypeReference>()?.type?.endsWith("TopicSlotComponent\$$component;") == true
                }
                val resultRegister = getInstruction<TwoRegisterInstruction>(index).registerA
                replaceInstruction(index, "const/4 v$resultRegister, 0x0")
            }
        }
        FootballCampaignFingerprint.classDef.forceBooleanGetterFalse("exposure")
    }
}