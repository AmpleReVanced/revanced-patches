package app.revanced.patches.kakaotalk.feature

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.feature.fingerprints.getFeatureFlagValueFingerprint
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val overrideFeatureFlagPatch = bytecodePatch(
    name = "Override feature flag",
    description = "Overrides the feature flag to enable the feature.",
    use = false
) {
    compatibleWith("com.kakao.talk"("25.10.0"))
    dependsOn(addExtensionPatch)

    execute {
        val method = getFeatureFlagValueFingerprint.method
        val parameterType = method.parameterTypes[0]
        val invokeStaticIdx = method.instructions.indexOfFirst { it.opcode == Opcode.INVOKE_STATIC }

        method.addInstructionsWithLabels(
            invokeStaticIdx,
            """
                invoke-virtual {p1}, ${parameterType}->getKey()Ljava/lang/String;
                move-result-object v0
                invoke-static {v0}, Lapp/revanced/extension/kakaotalk/feature/Flag;->canIntercept(Ljava/lang/String;)Z
                move-result v1
                if-eqz v1, :cond_original
                invoke-virtual {p1}, ${parameterType}->getKey()Ljava/lang/String;
                move-result-object v0
                invoke-static {v0}, Lapp/revanced/extension/kakaotalk/feature/Flag;->intercept(Ljava/lang/String;)Z
                move-result p1
                return p1
                :cond_original
                nop
            """.trimIndent()
        )
    }
}