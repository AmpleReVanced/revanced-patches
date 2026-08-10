package app.revanced.patches.kakaotalk.misc.integrity

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.AdFitEmulatorCheckFingerprint
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.AdFitRootCheckFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.parameterTypeNames
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("unused")
val disableAdFitEnvironmentDetectionPatch = bytecodePatch(
    name = "Disable AdFit environment detection",
    description = "Stops the AdFit ad SDK from reporting a root or emulator verdict to its " +
            "telemetry and ad request endpoints.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val publicStatic = AccessFlags.PUBLIC.value or AccessFlags.STATIC.value

        mutableClassDefBy(AdFitRootCheckFingerprint.classDef).methods.first { method ->
            method.accessFlags and publicStatic == publicStatic &&
                    method.parameterTypeNames == listOf("Landroid/content/Context;") &&
                    method.returnType == "Z"
        }.returnEarly(false)

        AdFitEmulatorCheckFingerprint.method.returnEarly(false)
    }
}