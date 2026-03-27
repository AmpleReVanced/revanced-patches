package app.revanced.patches.kakaotalk.integrity

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.integrity.fingerprints.IntentResolveClientMethod
import app.revanced.patches.kakaotalk.integrity.fingerprints.VerifyingSignatureFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val verifyingSignaturePatch = bytecodePatch(
    name = "Disable verifying signature",
    description = "Disables the signature verification check that prevents the app from running.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        VerifyingSignatureFingerprint.method.returnEarly(true)

        IntentResolveClientMethod.method.returnEarly(true)
    }
}