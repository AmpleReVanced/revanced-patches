package app.revanced.patches.kakaotalk.integrity

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.integrity.fingerprints.intentResolveClientMethod
import app.revanced.patches.kakaotalk.integrity.fingerprints.verifyingSignatureFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val verifyingSignaturePatch = bytecodePatch(
    name = "Disable verifying signature",
    description = "Disables the signature verification check that prevents the app from running.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        verifyingSignatureFingerprint.method.returnEarly(true)

        intentResolveClientMethod.method.returnEarly(true)
    }
}