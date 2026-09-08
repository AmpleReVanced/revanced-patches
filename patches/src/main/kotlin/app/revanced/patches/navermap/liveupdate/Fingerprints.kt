package app.revanced.patches.navermap.liveupdate

import app.morphe.patcher.Fingerprint

internal object OngoingActivitySupportFingerprint : Fingerprint(
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Z",
    strings = listOf("com.samsung.feature.nowbar"),
)

internal object OngoingActivityExtrasFingerprint : Fingerprint(
    strings = listOf("android.ongoingActivityNoti.style", "ongoingActivity"),
    custom = { method, _ ->
        method.parameterTypes.size == 3 &&
            method.parameterTypes[1] == "Landroid/content/Context;" &&
            method.returnType == method.parameterTypes[0].toString()
    },
)

internal class NotificationBuildFingerprint(builderClass: String) : Fingerprint(
    definingClass = builderClass,
    parameters = emptyList(),
    returnType = "Landroid/app/Notification;",
)