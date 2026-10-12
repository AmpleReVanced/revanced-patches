package app.revanced.patches.chzzk.tongpow

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.chzzk.shared.Constants.COMPATIBILITY_CHZZK
import app.revanced.util.parameterTypeNames
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("unused")
val autoClaimTongPowPatch = bytecodePatch(
    name = "Auto claim TongPow",
    description = "Automatically claims CHZZK TongPow rewards when they become available while watching streams.",
) {
    compatibleWith(COMPATIBILITY_CHZZK)

    execute {
        val method = TongPowChatEventFingerprint.method
        val insertion = resolveTongPowClaimInsertion(
            TongPowChatEventFingerprint.originalClassDef,
            method,
            TongPowManualClaimFingerprint.method,
        )
        Fingerprint(
            definingClass = insertion.successConstructor.definingClass,
            name = "<init>",
            parameters = insertion.successConstructor.parameterTypeNames,
        ).classDef.apply {
            accessFlags = accessFlags or AccessFlags.PUBLIC.value
        }
        val channel = insertion.channelRegister
        val claim = insertion.claimRegister
        val flow = insertion.flowRegister
        val scope = insertion.scopeRegister
        val callback = insertion.callbackRegister
        val constant = insertion.constantRegister

        method.addInstructionsWithLabels(
            insertion.dispatchIndex + 1,
            """
                if-eqz v$channel, :auto_claim_tong_pow_skip
                if-eqz v$claim, :auto_claim_tong_pow_skip
                iget-object v$scope, p0, ${insertion.serviceField.smaliReference}
                invoke-interface {v$scope, v$channel, v$claim}, ${insertion.claimCall.smaliReference}
                move-result-object v$flow
                invoke-static {v$flow}, ${insertion.retryCall.smaliReference}
                move-result-object v$flow
                invoke-virtual {p0}, ${insertion.scopeCall.smaliReference}
                move-result-object v$scope
                new-instance v$callback, ${insertion.callbackConstructor.definingClass}
                const/16 v$constant, ${insertion.callbackCase}
                invoke-direct {v$callback, p0, v$constant}, ${insertion.callbackConstructor.smaliReference}
                new-instance v$channel, ${insertion.successConstructor.definingClass}
                const/4 v$constant, 0x0
                invoke-direct {v$channel, v$callback, v$constant}, ${insertion.successConstructor.smaliReference}
                new-instance v$callback, ${insertion.errorConstructor.definingClass}
                const/16 v$constant, ${insertion.errorCase}
                invoke-direct {v$callback, v$constant}, ${insertion.errorConstructor.smaliReference}
                invoke-static {v$flow, v$scope, v$channel, v$callback}, ${insertion.collectCall.smaliReference}
                move-result-object v$flow
                invoke-virtual {p0, v$flow}, ${insertion.registerJobCall.smaliReference}
                :auto_claim_tong_pow_skip
                nop
            """.trimIndent(),
        )
    }
}
