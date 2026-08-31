package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.revanced.patches.kakaotalk.ad.fingerprints.MeasuringBizBoardFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.parameterRegister

@Suppress("unused")
val removeBizBoardPatch = bytecodePatch(
    name = "Remove BizBoard ads",
    description = "Removes the BizBoard ad by forcing its dimensions to 0x0 and visibility to GONE in onMeasure.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val method = MeasuringBizBoardFingerprint.method
        val receiverRegister = method.parameterRegister(0) - 1
        val registers = method.getFreeRegisterProvider(0, 2, receiverRegister)
        val viewRegister = registers.getFreeRegister4Bit()
        val valueRegister = registers.getFreeRegister4Bit()

        method.addInstructions(
            0,
            """
                move-object/from16 v$viewRegister, p0
                const/4 v$valueRegister, 0x0
                invoke-virtual {v$viewRegister, v$valueRegister, v$valueRegister}, Landroid/view/View;->setMeasuredDimension(II)V

                const/16 v$valueRegister, 0x8
                invoke-virtual {v$viewRegister, v$valueRegister}, Landroid/view/View;->setVisibility(I)V

                return-void
            """.trimIndent()
        )
    }
}
