package app.revanced.patches.chzzk.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.chzzk.shared.Constants.COMPATIBILITY_CHZZK

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
        MapClipAdCardFingerprint.method.returnEarly(null)
    }
}