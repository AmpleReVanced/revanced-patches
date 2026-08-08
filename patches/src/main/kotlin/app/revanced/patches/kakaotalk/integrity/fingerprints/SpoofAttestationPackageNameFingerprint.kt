package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object AttestationPayloadFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    strings = listOf("getPackageName(...)"),
    filters = listOf(
        methodCall("Landroid/content/Context;->getPackageName()Ljava/lang/String;"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "AbuseDetectUtil.kt" },
)
