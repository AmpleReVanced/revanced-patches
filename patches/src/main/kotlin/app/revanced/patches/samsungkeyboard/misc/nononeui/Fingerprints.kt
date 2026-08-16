package app.revanced.patches.samsungkeyboard.misc.nononeui

import app.morphe.patcher.Fingerprint

private val storeRequestStrings = listOf("&deviceId=", "&abiType=", "&oneUiVersion=")

internal object StoreDownloadRequestFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf(
        "Landroid/content/Context;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "Z",
        "Z",
    ),
    strings = storeRequestStrings,
)

internal object StoreUpdateCheckRequestFingerprint : Fingerprint(
    returnType = "[I",
    parameters = listOf("Landroid/content/Context;", "I", "Ljava/lang/String;"),
    strings = storeRequestStrings,
)

internal object ShowSoftInputFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I", "Landroid/content/Context;"),
    strings = listOf("showSoftInputInner flags="),
)
