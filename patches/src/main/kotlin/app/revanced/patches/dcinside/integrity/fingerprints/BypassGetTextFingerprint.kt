package app.revanced.patches.dcinside.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object NativeGetTextFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.NATIVE),
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
    returnType = "Ljava/lang/String;",
)