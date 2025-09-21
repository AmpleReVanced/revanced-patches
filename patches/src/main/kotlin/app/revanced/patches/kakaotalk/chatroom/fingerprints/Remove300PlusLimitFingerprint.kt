package app.revanced.patches.kakaotalk.chatroom.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("unused")
internal val limit300PlusBaseChatRoomFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters("Lcom/kakao/talk/widget/ViewBindable;")
    strings("300+")
}

@Suppress("unused")
internal val limit300PlusOpenChatRoomFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters()
    strings("300+")
}