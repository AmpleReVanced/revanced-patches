package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val chatLogFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Ljava/lang/String;")
    parameters()
    strings(
        "[class:",
        "] ChatLog [id=",
    )
}