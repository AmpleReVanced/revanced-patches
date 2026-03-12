package app.revanced.patches.kakaotalk.feature

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.kakaotalk.feature.fingerprints.getFeatureFlagValueFingerprint
import app.revanced.patches.kakaotalk.feature.fingerprints.getFeatureFlagsInExtensionFingerprint
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val overrideFeatureFlagPatch = bytecodePatch(
    name = "Override feature flag",
    description = "Overrides the feature flag to enable the feature.",
    use = false
) {
    compatibleWith("com.kakao.talk"("26.2.1"))
    dependsOn(addExtensionPatch)

    // Example: "normal_chat_room_comment_disabled=false;open_chat_room_comment_disabled=false"
    val overrideFeatureFlag by stringOption(
        key = "featureFlags",
        title = "Feature flag overrides",
        description = "Enter feature flag overrides as semicolon-separated key=value pairs.",
    )

    execute {
        getFeatureFlagsInExtensionFingerprint.method.apply {
            val featureFlags = overrideFeatureFlag?.takeIf { it.isNotBlank() }

            addInstructions(
                0,
                """
                    const-string v0, "$featureFlags"
                    return-object v0
                """.trimIndent()
            )
        }

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