package app.revanced.patches.dcinside.misc.updates

import app.morphe.patcher.Fingerprint

internal object CheckIsRecentVersionFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/String;"),
    returnType = "Z",
    strings = listOf("updateIgnoreCase", "popup_interval_update"),
)