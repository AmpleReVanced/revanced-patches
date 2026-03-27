package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ads.fingerprints.FeedAdLayoutFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
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
        // We add setVisibility(View.GONE); before the constructor returns
        FeedAdLayoutFingerprint.method.addInstructionsWithLabels(
            FeedAdLayoutFingerprint.method.instructions.size - 1,
            """
                const/16 v0, 0x8
                invoke-virtual {p0, v0}, ${FeedAdLayoutFingerprint.method.definingClass}->setVisibility(I)V
                
                # layoutParams = getLayoutParams()
                invoke-virtual {p0}, ${FeedAdLayoutFingerprint.method.definingClass}->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
                move-result-object v0
                if-eqz v0, :skipSet
        
                const/4 v1, 0x0
                iput v1, v0, Landroid/view/ViewGroup${"$"}LayoutParams;->height:I
                iput v1, v0, Landroid/view/ViewGroup${"$"}LayoutParams;->width:I
        
                :skipSet
                invoke-virtual {p0}, ${FeedAdLayoutFingerprint.method.definingClass}->requestLayout()V
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