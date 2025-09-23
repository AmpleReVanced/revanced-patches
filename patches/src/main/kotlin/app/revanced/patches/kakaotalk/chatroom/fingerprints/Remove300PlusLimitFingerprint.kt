package app.revanced.patches.kakaotalk.chatroom.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("unused")
internal val limit300PlusBaseChatRoomFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters()
    strings("300+")
    custom { method, classDef -> classDef.sourceFile == "BaseChatRoomItem.kt" }
}

@Suppress("unused")
internal val limit300PlusOpenChatRoomFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    parameters()
    strings("300+")
    custom { method, classDef -> classDef.sourceFile == "OpenLinkChatsItem.kt" }
}