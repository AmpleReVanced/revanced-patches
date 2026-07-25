package app.revanced.patches.chzzk.cheatkey

import app.morphe.patcher.Fingerprint

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
        "StreamingChannelMyInfo(_userRole=",
        ", cheatKey=",
    ),
)