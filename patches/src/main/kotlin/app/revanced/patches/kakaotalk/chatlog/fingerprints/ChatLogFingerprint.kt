package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ChatLogFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf(
        "[class:",
        "] ChatLog [id=",
    )
)