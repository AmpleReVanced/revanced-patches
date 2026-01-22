package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.revanced.patcher.fingerprint

internal const val CHAT_INFO_EXTENSION_TYPE = "Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;"

internal val getDeletedColorFingerprint = fingerprint {
    custom { method, classDef -> classDef.type == CHAT_INFO_EXTENSION_TYPE && method.name == "getDeletedColor" }
}

internal val getHiddenColorFingerprint = fingerprint {
    custom { method, classDef -> classDef.type == CHAT_INFO_EXTENSION_TYPE && method.name == "getHiddenColor" }
}