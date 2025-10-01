package app.revanced.patches.kakaotalk.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ads.fingerprints.measuringBizBoardFingerprint

@Suppress("unused")
val removeBizBoardPatch = bytecodePatch(
    name = "Remove BizBoard ads",
    description = "Removes the BizBoard ad by forcing its dimensions to 0x0 and visibility to GONE in onMeasure.",
) {
    compatibleWith("com.kakao.talk"("25.8.2"))

    execute {
        val method = measuringBizBoardFingerprint.method

        method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-virtual {p0, v0, v0}, Landroid/view/View;->setMeasuredDimension(II)V

                const/16 v0, 0x8
                invoke-virtual {p0, v0}, Landroid/view/View;->setVisibility(I)V

                return-void
            """.trimIndent()
        )
    }
}