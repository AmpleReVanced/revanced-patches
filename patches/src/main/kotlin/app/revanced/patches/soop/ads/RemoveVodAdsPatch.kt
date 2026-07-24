package app.revanced.patches.soop.ads

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.soop.ads.fingerprints.*
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
val removeVodAdsPatch = bytecodePatch(
    name = "Remove VOD ads",
    description = "Removes VOD pre-roll ads by reporting every stream as QuickView Plus to the " +
        "player's own ad-skip check.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        val check = VodQuickViewCheckFingerprint.method
        val instructions = check.instructions.toList()

        val statusLiteralIndex = instructions.indexOfFirst {
            ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "QUICKVIEW_PLUS"
        }
        if (statusLiteralIndex < 0) throw PatchException("Could not find the QuickView status literal.")

        val statusGetter = (statusLiteralIndex - 1 downTo 0)
            .asSequence()
            .mapNotNull { (instructions[it] as? ReferenceInstruction)?.reference as? MethodReference }
            .firstOrNull { it.returnType == "Ljava/lang/String;" && it.parameterTypes.isEmpty() }
            ?: throw PatchException("Could not resolve the QuickView status getter.")

        mutableClassDefBy(statusGetter.definingClass).methods
            .firstOrNull { MethodUtil.methodSignaturesMatch(it, statusGetter) }
            ?.returnEarly("QUICKVIEW_PLUS")
            ?: throw PatchException("Could not resolve the mutable QuickView status getter.")
    }
}