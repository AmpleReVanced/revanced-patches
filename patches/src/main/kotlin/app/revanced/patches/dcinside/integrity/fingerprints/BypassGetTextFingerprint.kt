package app.revanced.patches.dcinside.integrity.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal val nativeGetTextFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.NATIVE)
    parameters("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;")
    returns("Ljava/lang/String;")
}