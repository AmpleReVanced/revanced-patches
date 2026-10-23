package app.revanced.patches.chzzk.cheatkey

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType.CONTAINS
import app.morphe.patcher.string

internal object CheatKeyStatusFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf(
        "CheatKeyStatus(cheatKeyHistoryExist=",
        ", subscribing=",
    ),
)

internal object StreamingChannelMyInfoFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf(
        string("StreamingChannelMyInfo(", CONTAINS),
        string(", cheatKey=", CONTAINS),
    ),
)