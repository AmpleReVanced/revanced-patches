package app.revanced.patches.riroschool.misc.analytics

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

/** Resolve the persisted UUID getter by its storage key instead of its class or method name. */
internal fun identityUuidGetterFingerprint() = Fingerprint(
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Ljava/lang/String;",
    strings = listOf("SAVE_KEY_UUID"),
)

/** Match native form builders even after Firebase has removed their FCM fields. */
internal fun nativeIdentityFormsFingerprint() = Fingerprint(
    returnType = "[B",
    strings = listOf("&device_id=", "&app_version=", "&cookie_token="),
    filters = listOf(methodCall("Ljava/lang/String;->getBytes()[B")),
    custom = { method, _ -> method.parameterTypes.firstOrNull() == "Landroid/content/Context;" },
)
