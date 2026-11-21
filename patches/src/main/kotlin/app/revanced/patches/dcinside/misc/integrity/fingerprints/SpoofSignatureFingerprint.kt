package app.revanced.patches.dcinside.misc.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

// sh
internal object NativeGetSignatureHexFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.NATIVE),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Ljava/lang/String;",
)

// vd
internal object NativeGetSignatureByTypeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.NATIVE),
    parameters = listOf("Ljava/lang/String;"),
    returnType = "Ljava/util/ArrayList;",
)

internal object GenerateXAndroidCertFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("SHA-1", "Package info not found."),
    filters = listOf(methodCall("Ljava/lang/StringBuilder;->toString()Ljava/lang/String;")),
)
