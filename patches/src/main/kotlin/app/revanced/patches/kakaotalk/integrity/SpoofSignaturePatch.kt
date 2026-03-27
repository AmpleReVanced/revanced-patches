package app.revanced.patches.kakaotalk.integrity

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.integrity.fingerprints.UtilityGetSignatureFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Spoofs the app signature to pass integrity checks.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        UtilityGetSignatureFingerprint.method.returnEarly("7MRbkCrB6DyL4XWKJX5nSS3jdFY=")
    }
}