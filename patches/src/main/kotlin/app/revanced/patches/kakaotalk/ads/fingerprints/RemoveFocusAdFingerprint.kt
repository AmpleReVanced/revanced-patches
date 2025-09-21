package app.revanced.patches.kakaotalk.ads.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val loadFocusAdFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    parameters("Lcom/kakao/adfit/ads/focus/FocusAdLoader\$OnAdLoadListener;")
    returns("Z")
    strings("listener", " owner is destroyed.", " loading is already started.", "Request Focus AD", " loading is started.", "Focus ad is cached. [id = ")
    opcodes(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IF_NE,
    )
}