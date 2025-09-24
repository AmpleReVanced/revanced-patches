package app.revanced.patches.kakaotalk.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.kakaotalk.ads.fingerprints.feedAdLayoutFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Suppress("unused")
val removeFeedAdPatch = bytecodePatch(
    name = "Remove feed ad",
    description = "Removes the feed ad from the app.",
) {
    compatibleWith("com.kakao.talk"("25.8.0"))

    execute {
        val feedAdLayoutConstructor = feedAdLayoutFingerprint.method
        val feedAdLayoutClass = feedAdLayoutFingerprint.classDef

        // We add setVisibility(View.GONE); before the constructor returns
        feedAdLayoutConstructor.addInstructionsWithLabels(
            feedAdLayoutConstructor.instructions.size - 1,
            """
                const/16 v0, 0x8
                invoke-virtual {p0, v0}, ${feedAdLayoutConstructor.definingClass}->setVisibility(I)V
                
                # layoutParams = getLayoutParams()
                invoke-virtual {p0}, ${feedAdLayoutConstructor.definingClass}->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
                move-result-object v0
                if-eqz v0, :skipSet
        
                const/4 v1, 0x0
                iput v1, v0, Landroid/view/ViewGroup${"$"}LayoutParams;->height:I
                iput v1, v0, Landroid/view/ViewGroup${"$"}LayoutParams;->width:I
        
                :skipSet
                invoke-virtual {p0}, ${feedAdLayoutConstructor.definingClass}->requestLayout()V
            """.trimIndent()
        )

        feedAdLayoutClass.methods.filter { it.name == "setFeedAd" }.forEach { method ->
            method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent()
            )
        }

        feedAdLayoutClass.methods.add(
            ImmutableMethod(
                feedAdLayoutConstructor.definingClass,
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
                    """
                        const/4 v0, 0x0
                        invoke-virtual {p0, v0, v0}, ${feedAdLayoutClass.type}->setMeasuredDimension(II)V
                        return-void
                    """.trimIndent()
                )
            }
        )
    }
}