package app.revanced.patches.kakaotalk.ads.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val loadNativeAdFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("I", "Lcom/kakao/adfit/ads/media/NativeAdLoader\$AdLoadListener;")
    returns("Z")
    strings(
        "listener",
        " owner is destroyed.",
        " loading is already started.",
        "Request Native AD",
        " loading is started.",
        "Native ad is cached. [id = ",
        "] [dsp = ",
        "] [count = ",
        "Invalid Count: "
    )
    opcodes(
        Opcode.MOVE_FROM16,
        Opcode.CONST_STRING,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.INVOKE_STATIC,
        Opcode.IF_LEZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IF_NE,
    )
    custom { method, classDef -> method.name == "load" }
}