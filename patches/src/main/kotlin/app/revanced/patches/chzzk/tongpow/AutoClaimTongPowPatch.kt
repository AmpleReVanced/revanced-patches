package app.revanced.patches.chzzk.tongpow

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.chzzk.shared.Constants.COMPATIBILITY_CHZZK
import app.revanced.util.smaliReference

@Suppress("unused")
val autoClaimTongPowPatch = bytecodePatch(
    name = "Auto claim TongPow",
    description = "Automatically claims CHZZK TongPow rewards when they become available while watching streams.",
) {
    compatibleWith(COMPATIBILITY_CHZZK)

    execute {
        val insertion = TongPowAutoClaimInsertion.resolve(
            popupEventCollectorClass = TongPowPopupEventCollectorFingerprint.originalClassDef,
            popupEventCollectorMethod = TongPowPopupEventCollectorFingerprint.method,
            manualClaimMethod = TongPowManualClaimFingerprint.method,
        )

        insertion.method.addInstructionsWithLabels(
            insertion.showPopupIndex + 1,
            """
                if-eqz v${insertion.registers.flag}, :auto_claim_tong_pow_hide_skip
                invoke-virtual/range {v${insertion.showPopupReceiverRegister} .. v${insertion.showPopupReceiverRegister}}, ${insertion.manualClaim.hidePopup.smaliReference}
                :auto_claim_tong_pow_hide_skip
                nop
            """.trimIndent(),
        )

        insertion.method.addInstructionsWithLabels(
            insertion.updatePopupInfoIndex + 1,
            """
                const/4 v${insertion.registers.flag}, 0x0
                if-eqz v${insertion.channelIdRegister}, :auto_claim_tong_pow_skip
                if-eqz v${insertion.claimIdRegister}, :auto_claim_tong_pow_skip
                new-instance v${insertion.registers.callback}, ${insertion.manualClaim.callbackConstructor.definingClass}
                iget-object v${insertion.registers.scratch}, v0, ${insertion.chatViewModelField.smaliReference}
                invoke-direct {v${insertion.registers.callback}, v${insertion.registers.scratch}}, ${insertion.manualClaim.callbackConstructor.smaliReference}
                iget-object v${insertion.registers.scratch}, v0, ${insertion.popupViewModelField.smaliReference}
                invoke-virtual {v${insertion.registers.scratch}, v${insertion.channelIdRegister}, v${insertion.claimIdRegister}, v${insertion.registers.callback}}, ${insertion.manualClaim.receiveAmount.smaliReference}
                const/4 v${insertion.registers.flag}, 0x1
                :auto_claim_tong_pow_skip
                nop
            """.trimIndent(),
        )
    }
}
