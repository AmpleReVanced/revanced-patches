package app.revanced.patches.kakaotalk.feature

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.morphe.patches.shared.misc.settings.preference.TextPreference
import com.android.tools.smali.dexlib2.Opcode

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/OverrideFeatureFlagPatch;"

@Suppress("unused")
val overrideFeatureFlagPatch = bytecodePatch(
    name = "Override feature flag",
    description = "Overrides the feature flag to enable the feature.",
//    default = false
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    // Example: "normal_chat_room_comment_disabled=false;open_chat_room_comment_disabled=false"
    val overrideFeatureFlag by stringOption(
        key = "featureFlags",
        title = "Feature flag overrides",
        description = "Enter feature flag overrides as semicolon-separated key=value pairs.",
    )

    execute {
        PreferenceScreen.CHAT.addPreferences(
            TextPreference(
                key = "morphe_pref_feature_flag_overrides",
                titleKey = "morphe_settings_patch_feature_flag_overrides",
                summaryKey = "morphe_settings_patch_feature_flag_overrides_summary",
                tag = "EditTextPreference",
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        GetFeatureFlagsInExtensionFingerprint.method.apply {
            val featureFlags = overrideFeatureFlag?.takeIf { it.isNotBlank() }

            addInstructions(
                0,
                """
                    const-string v0, "$featureFlags"
                    return-object v0
                """.trimIndent()
            )
        }

        val method = GetFeatureFlagValueFingerprint.method
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
