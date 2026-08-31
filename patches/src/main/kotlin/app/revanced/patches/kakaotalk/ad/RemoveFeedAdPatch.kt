package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ad.fingerprints.FeedAdLayoutFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.parameterRegister
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Suppress("unused")
val removeFeedAdPatch = bytecodePatch(
    name = "Remove feed ad",
    description = "Removes the feed ad from the app.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val constructor = FeedAdLayoutFingerprint.method
        val insertionIndex = constructor.instructions.size - 1
        val receiverRegister = constructor.parameterRegister(0) - 1
        val registers = constructor.getFreeRegisterProvider(insertionIndex, 2, receiverRegister)
        val valueRegister = registers.getFreeRegister4Bit()
        val layoutParamsRegister = registers.getFreeRegister4Bit()

        constructor.addInstructionsWithLabels(
            insertionIndex,
            """
                const/16 v$valueRegister, 0x8
                invoke-virtual {p0, v$valueRegister}, ${constructor.definingClass}->setVisibility(I)V
                invoke-virtual {p0}, ${constructor.definingClass}->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
                move-result-object v$layoutParamsRegister
                if-eqz v$layoutParamsRegister, :skipSet
                const/4 v$valueRegister, 0x0
                iput v$valueRegister, v$layoutParamsRegister, Landroid/view/ViewGroup${"$"}LayoutParams;->height:I
                iput v$valueRegister, v$layoutParamsRegister, Landroid/view/ViewGroup${"$"}LayoutParams;->width:I
                :skipSet
                invoke-virtual {p0}, ${constructor.definingClass}->requestLayout()V
            """.trimIndent()
        )

        FeedAdLayoutFingerprint.classDef.methods.filter {
            it.name == "setFeedAd"
        }.forEach { method ->
            method.returnEarly()
        }

        FeedAdLayoutFingerprint.classDef.methods.add(
            ImmutableMethod(
                FeedAdLayoutFingerprint.method.definingClass,
                "onMeasure",
                listOf(
                    ImmutableMethodParameter("I", null, null),
                    ImmutableMethodParameter("I", null, null)
                ),
                "V",
                AccessFlags.PROTECTED.value,
                null,
                null,
                MutableMethodImplementation(5)
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        invoke-virtual {p0, v0, v0}, ${FeedAdLayoutFingerprint.classDef.type}->setMeasuredDimension(II)V
                        return-void
                    """.trimIndent()
                )
            }
        )
    }
}