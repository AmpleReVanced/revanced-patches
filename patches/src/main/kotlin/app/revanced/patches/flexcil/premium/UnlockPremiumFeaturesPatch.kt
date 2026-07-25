package app.revanced.patches.flexcil.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.returnEarly
import app.revanced.patches.flexcil.shared.Constants.COMPATIBILITY_FLEXCIL
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private fun MutableMethod.unlockGate() {
    val instructions = implementation?.instructions?.toList().orEmpty()
    val negatedReturn = instructions.withIndex().any { (index, instruction) ->
        if (instruction.opcode != Opcode.XOR_INT_LIT8 ||
            (instruction as NarrowLiteralInstruction).narrowLiteral != 1
        ) {
            return@any false
        }

        val xorRegister = (instruction as OneRegisterInstruction).registerA
        val next = instructions.getOrNull(index + 1) ?: return@any false
        next.opcode == Opcode.RETURN && (next as OneRegisterInstruction).registerA == xorRegister
    }
    returnEarly(!negatedReturn)
}

@Suppress("unused")
val unlockPremiumFeaturesPatch = bytecodePatch(
    name = "Unlock Premium features",
    description = "Enables app features locked behind the subscription paywall.",
) {
    compatibleWith(COMPATIBILITY_FLEXCIL)

    execute {
        PremiumProductsGateFingerprint.matchAll().forEach { it.method.unlockGate() }
        AccountPremiumGateFingerprint.matchAll().forEach { it.method.unlockGate() }
        ActiveSubscriptionFingerprint.method.returnEarly(true)
        B2bLicenseFingerprint.method.returnEarly(true)
        OwnsQueriedProductFingerprint.method.returnEarly(true)
    }
}