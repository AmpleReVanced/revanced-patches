package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal val replaceToFeedFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returns("V")
    strings(
        "feedType",
        "{}",
        "safeBot",
        "getString(...)",
        "hidden",
        "byHost",
        "previous_message",
        "previous_enc",
        "enc : %s, %s",
    )
}