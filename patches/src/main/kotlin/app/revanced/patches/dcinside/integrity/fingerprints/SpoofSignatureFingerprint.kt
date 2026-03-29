package app.revanced.patches.dcinside.integrity.fingerprints

import app.morphe.patcher.Fingerprint
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