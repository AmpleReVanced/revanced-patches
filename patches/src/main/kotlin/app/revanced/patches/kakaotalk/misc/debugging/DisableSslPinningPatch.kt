package app.revanced.patches.kakaotalk.misc.debugging

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.all.misc.extension.SHARED_UTILS_EXTENSION_CLASS
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.debugging.fingerprints.CertificatePinCheckFingerprint
import app.revanced.patches.kakaotalk.misc.debugging.fingerprints.DefaultTrustManagerFingerprint
import app.revanced.patches.kakaotalk.misc.debugging.fingerprints.SslDebugApplicationContextFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode

internal const val SSL_DEBUG_EXTENSION_CLASS = "Lapp/revanced/extension/kakaotalk/patches/DisableSslPinningPatch;"

@Suppress("unused")
val disableSslPinningPatch = patch(
    name = "Disable SSL pinning",
    description = "Adds a debug option to inspect OkHttp and WebView HTTPS traffic using an installed proxy CA certificate.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.DEBUG.addPreferences(
            SwitchPreference(
                key = "morphe_pref_disable_ssl_pinning",
                titleKey = "morphe_settings_patch_disable_ssl_pinning",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(SSL_DEBUG_EXTENSION_CLASS)
        allowWebViewProxyCertificates()

        SslDebugApplicationContextFingerprint.method.apply {
            addInstruction(
                indexOfFirstInstructionOrThrow(Opcode.RETURN_VOID),
                "invoke-static/range {p0 .. p0}, $SHARED_UTILS_EXTENSION_CLASS->setContext(Landroid/content/Context;)V",
            )
        }

        CertificatePinCheckFingerprint.method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $SSL_DEBUG_EXTENSION_CLASS->isEnabled()Z
                    move-result v$register
                    if-eqz v$register, :original
                    return-void
                """,
                ExternalLabel("original", getInstruction(0)),
            )
        }

        DefaultTrustManagerFingerprint.method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $SSL_DEBUG_EXTENSION_CLASS->getProxyTrustManager()Ljavax/net/ssl/X509TrustManager;
                    move-result-object v$register
                    if-eqz v$register, :original
                    return-object v$register
                """,
                ExternalLabel("original", getInstruction(0)),
            )
        }
    }
}
