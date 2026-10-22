package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType.CONTAINS
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MainBoardAdBindFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf(string("bind() isAdFetched - ", CONTAINS), string("checkAdType() type - ", CONTAINS)),
)

internal object MainBoardAdRequestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(
            parameters = listOf("L", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
            returnType = SAM_QUERY_PARAMS,
            opcode = Opcode.INVOKE_STATIC,
        ),
    ) + samAdResponseFilters(),
)