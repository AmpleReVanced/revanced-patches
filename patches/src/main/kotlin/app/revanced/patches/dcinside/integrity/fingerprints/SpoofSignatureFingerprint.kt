package app.revanced.patches.dcinside.integrity.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// sh
internal val nativeGetSignatureHexFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.NATIVE)
    parameters("Landroid/content/Context;")
    returns("Ljava/lang/String;")
}

// vd
internal val nativeGetSignatureByTypeFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.NATIVE)
    parameters("Ljava/lang/String;")
    returns("Ljava/util/ArrayList;")
}