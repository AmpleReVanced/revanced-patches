package app.revanced.patches.kakaotalk.misc.feature

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.misc.settings.preference.TextPreference
import app.morphe.util.cloneMutable
import app.morphe.util.cloneParameters
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags

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

        val hashMethod = HashFeatureKeyFingerprint.method
        val hasherInstance = HashFeatureKeyFingerprint.classDef.fields.single {
            AccessFlags.STATIC.isSet(it.accessFlags) && it.type == hashMethod.definingClass
        }
        val hashBridge = HashFeatureKeyInExtensionFingerprint.method
        val expandedHashBridge = hashBridge.cloneMutable(additionalRegisters = 2)
        HashFeatureKeyInExtensionFingerprint.classDef.methods.apply {
            remove(hashBridge)
            add(expandedHashBridge)
        }
        expandedHashBridge.apply {
            val registers = getFreeRegisterProvider(0, 2)
            val instanceRegister = registers.getFreeRegister4Bit()
            val versionRegister = registers.getFreeRegister4Bit()
            addInstructions(
                0,
                """
                    sget-object v$instanceRegister, ${hasherInstance.smaliReference}
                    const v$versionRegister, ${packageMetadata.versionCode}
                    invoke-virtual {v$instanceRegister, p0, v$versionRegister}, ${hashMethod.smaliReference}
                    move-result-object v$instanceRegister
                    return-object v$instanceRegister
                """,
            )
        }

        val method = GetFeatureFlagValueFingerprint.method.cloneParameters()
        val registerProvider = method.getFreeRegisterProvider(0, 2)
        val keyRegister = registerProvider.getFreeRegister4Bit()
        val interceptedRegister = registerProvider.getFreeRegister4Bit()

        method.addInstructionsWithLabels(
            0,
            """
                invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;
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
