package app.revanced.patches.soop.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.soop.ads.fingerprints.*
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP
import com.android.tools.smali.dexlib2.Opcode

private fun MutableMethod.returnVoidAfterSuper() {
    val superIndex = instructions.indexOfFirst { it.opcode == Opcode.INVOKE_SUPER }
    if (superIndex < 0) throw PatchException("Could not find the super call in $name.")
    addInstructions(superIndex + 1, "return-void")
}

@Suppress("unused")
val removeBannerAdsPatch = bytecodePatch(
    name = "Remove banner ads",
    description = "Removes the VOD companion and search banner ads.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        CompanionBannerViewCreatedFingerprint.method.returnVoidAfterSuper()
        SearchAdViewCreatedFingerprint.method.returnVoidAfterSuper()
    }
}