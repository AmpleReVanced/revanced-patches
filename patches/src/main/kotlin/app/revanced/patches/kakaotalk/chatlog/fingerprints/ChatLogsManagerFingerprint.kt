package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal val replaceToFeedFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    strings(
        "chatLog",
        "feedType",
        "byHost",
        "no matched overwrite feedType : ",
    )
    custom { method, classDef -> classDef.sourceFile == "ChatLogsManager.kt" }
}