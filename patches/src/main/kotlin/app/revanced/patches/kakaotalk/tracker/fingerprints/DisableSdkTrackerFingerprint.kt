package app.revanced.patches.kakaotalk.tracker.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal val buildSdkTrackerUrlFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters()
    returns("Ljava/lang/String;")
    strings(
        "id",
        "sdktype",
        "sdkver",
        "cnt",
        "test",
        "Y",
        "ctag",
        "ukeyword", // ...And more
    )
    custom { method, classDef -> classDef.type.startsWith("Lcom/kakao/adfit") }
}