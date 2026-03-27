package app.revanced.patches.kakaotalk.tracker.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object BuildSdkTrackerUrlFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/adfit",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Ljava/lang/String;",
    strings = listOf(
        "id",
        "sdktype",
        "sdkver",
        "cnt",
        "test",
        "Y",
        "ctag",
        "ukeyword", // ...And more
    )
)