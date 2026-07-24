package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.Fingerprint

internal object MainBoardAdBindFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("bind() isAdFetched - ", "checkAdType() type - "),
)