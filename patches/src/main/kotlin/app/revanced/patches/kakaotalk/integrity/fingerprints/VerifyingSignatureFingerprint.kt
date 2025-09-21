package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("unused")
internal val verifyingSignatureFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returns("Z")
    parameters()
    strings("getPackageName(...)")
}