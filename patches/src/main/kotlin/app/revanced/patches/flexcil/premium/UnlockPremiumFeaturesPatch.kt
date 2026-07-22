package app.revanced.patches.flexcil.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.returnEarly
import app.revanced.patches.flexcil.shared.Constants.COMPATIBILITY_FLEXCIL
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction

private fun MutableMethod.unlockGate() {
    val negated = implementation?.instructions?.any {
        it.opcode == Opcode.XOR_INT_LIT8 && (it as? NarrowLiteralInstruction)?.narrowLiteral == 1
    } == true
    returnEarly(!negated)
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