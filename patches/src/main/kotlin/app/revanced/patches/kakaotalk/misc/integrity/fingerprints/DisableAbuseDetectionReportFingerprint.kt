package app.revanced.patches.kakaotalk.misc.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object AbuseDetectReportSenderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/String;"),
    returnType = "V",
    strings = listOf("nonceData"),
    custom = { _, classDef -> classDef.sourceFile == "AbuseDetectUtil.kt" }
)

internal object AbuseDetectIntegrityTokenFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/String;", "Lkotlin/coroutines/Continuation;"),
    returnType = "Ljava/lang/Object;",
    custom = { _, classDef -> classDef.sourceFile == "AbuseDetectUtil.kt" }
)
