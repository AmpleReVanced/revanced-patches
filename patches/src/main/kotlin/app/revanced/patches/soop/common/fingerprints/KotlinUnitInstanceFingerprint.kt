package app.revanced.patches.soop.common.fingerprints

import app.morphe.patcher.Fingerprint

internal object KotlinUnitInstanceFingerprint : Fingerprint(
    parameters = listOf(),
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("kotlin.Unit"),
)