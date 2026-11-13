package app.revanced.patches.riroschool.misc.integrity

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

/** Locate the runnable by its VPN diagnostics and public connectivity API. */
internal fun vpnDetectorFingerprint() = Fingerprint(
    parameters = emptyList(),
    returnType = "V",
    strings = listOf("connectivity", "VPN_CHECK", "VPN FOUND: "),
    filters = listOf(methodCall("Landroid/net/NetworkCapabilities;->hasTransport(I)Z")),
    custom = { method, _ -> method.name == "run" },
)

/** Locate the activity lifecycle method that owns both debug checks. */
internal fun adbDetectorFingerprint() = Fingerprint(
    parameters = emptyList(),
    returnType = "V",
    strings = listOf("adb_enabled"),
    filters = listOf(methodCall("Landroid/os/Debug;->isDebuggerConnected()Z")),
    custom = { method, owner -> method.name == "onResume" && owner.type == "Lcom/rirosoft/riroschool/views/activity/MainActivity;" },
)

/** Locate the startup availability consumer without its obfuscated method name. */
internal fun gmsAvailabilityFingerprint() = Fingerprint(
    parameters = emptyList(),
    returnType = "V",
    filters = listOf(methodCall("Lcom/google/android/gms/common/GoogleApiAvailability;->isGooglePlayServicesAvailable(Landroid/content/Context;)I")),
    custom = { _, owner -> owner.type == "Lcom/rirosoft/riroschool/views/activity/IntroActivity;" },
)
