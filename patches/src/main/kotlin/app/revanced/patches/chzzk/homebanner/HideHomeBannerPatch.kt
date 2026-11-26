package app.revanced.patches.chzzk.homebanner

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.patches.chzzk.common.utils.forceBooleanGetterFalse
import app.revanced.patches.chzzk.common.utils.forceObjectGetterNull
import app.revanced.patches.chzzk.shared.Constants.COMPATIBILITY_CHZZK
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val hideHomeBannerPatch = patch(
    name = "Hide home banners",
    description = "Hides promotional banners on the CHZZK home recommend tab, including the top " +
        "banner carousel and the in-feed event and image banners.",
) {
    compatibleWith(COMPATIBILITY_CHZZK)

    execute {
        StreamingHomeBannersFingerprint.classDef.forceObjectGetterNull("banners")
        TopicBannerRowsFingerprint.let {
            it.instructionMatches.forEach { banner ->
                val index = it.method.indexOfFirstInstructionReversedOrThrow(banner.index, Opcode.INSTANCE_OF)
                val register = it.method.getInstruction<TwoRegisterInstruction>(index).registerA
                it.method.replaceInstruction(index, "const/4 v$register, 0x0")
            }
        }
        FootballCampaignFingerprint.classDef.forceBooleanGetterFalse("exposure")
    }
}