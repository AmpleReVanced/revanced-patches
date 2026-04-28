package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.chatlog.fingerprints.CheckViewableChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.FilterChatLogItemFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.GetDeletedMessageCacheFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.PutDeletedMessageCacheFingerprint

context(patchContext: BytecodePatchContext)
internal fun keepDeletedAndHiddenChatLogsVisible() {
    CheckViewableChatLogFingerprint.method.returnEarly(true)
    FilterChatLogItemFingerprint.method.returnEarly(true)
    PutDeletedMessageCacheFingerprint.method.returnEarly()
    GetDeletedMessageCacheFingerprint.method.returnEarly(false)
}