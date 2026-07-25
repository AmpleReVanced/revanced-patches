package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXIT_AD_DIALOG =
    "Lkr/co/nowcom/mobile/afreeca/advertisement/a1/home/presenter/ExitAdDialog;"

private fun Method.callsMethod(name: String) =
    implementation?.instructions?.any { it.getReference<MethodReference>()?.name == name } == true

internal object ExitAdRevealFingerprint : Fingerprint(
    parameters = listOf(),
    returnType = "V",
    definingClass = EXIT_AD_DIALOG,
    custom = { method, _ -> method.callsMethod("setLayoutParams") },
)

internal object ExitAdAnimateRevealFingerprint : Fingerprint(
    parameters = listOf(),
    returnType = "V",
    definingClass = EXIT_AD_DIALOG,
    custom = { method, _ -> method.callsMethod("startAnimation") },
)
