package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint

private const val CHAT_INFO_EXTENSION_TYPE = "Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;"

internal object GetDeletedColorFingerprint : Fingerprint(
    definingClass = CHAT_INFO_EXTENSION_TYPE,
    name = "getDeletedColor"
)

internal object GetHiddenColorFingerprint : Fingerprint(
    definingClass = CHAT_INFO_EXTENSION_TYPE,
    name = "getHiddenColor"
)