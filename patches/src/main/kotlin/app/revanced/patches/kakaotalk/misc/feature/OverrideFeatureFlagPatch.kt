package app.revanced.patches.kakaotalk.misc.feature

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.morphe.patches.shared.misc.settings.preference.TextPreference

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
        PreferenceScreen.ADVANCED.addPreferences(
            TextPreference(
                key = "morphe_pref_feature_flag_overrides",
                titleKey = "morphe_settings_patch_feature_flag_overrides",
                summaryKey = "morphe_settings_patch_feature_flag_overrides_summary",
                tag = "app.morphe.extension.shared.settings.preference.MorpheEditTextPreference",
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
        val registerProvider = method.getFreeRegisterProvider(0, 2)
        val keyRegister = registerProvider.getFreeRegister4Bit()
        val interceptedRegister = registerProvider.getFreeRegister4Bit()

        method.addInstructionsWithLabels(
            0,
            """
                invoke-virtual {p1}, ${parameterType}->getKey()Ljava/lang/String;
                move-result-object v$keyRegister
                invoke-static {v$keyRegister}, Lapp/revanced/extension/kakaotalk/feature/Flag;->canIntercept(Ljava/lang/String;)Z
                move-result v$interceptedRegister
                if-eqz v$interceptedRegister, :cond_original
                invoke-static {v$keyRegister}, Lapp/revanced/extension/kakaotalk/feature/Flag;->intercept(Ljava/lang/String;)Z
                move-result p1
                return p1
                :cond_original
                nop
            """.trimIndent()
        )
    }
}
