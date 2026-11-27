package app.revanced.patches.chzzk.keybox

import app.morphe.patcher.patch.patch
import app.revanced.patches.chzzk.shared.Constants.COMPATIBILITY_CHZZK
import app.revanced.patches.shared.misc.native.nativePatch

private const val KEY_BOX = "libczz_keybox.so"
private const val CHECK_FINGERPRINTS = "checkFingerprints"

private val fixKeyBoxNativePatch = nativePatch(COMPATIBILITY_CHZZK) {
    file("lib/arm64-v8a/$KEY_BOX") {
        replace(
            symbol = CHECK_FINGERPRINTS,
            fingerprint = "ff0302d1 fd7b02a9",
            expected = "ff0302d1",
            replacement = "20008052",
        )
        replace(
            symbol = CHECK_FINGERPRINTS,
            fingerprint = "fd7b02a9 fc6f03a9",
            expected = "fd7b02a9",
            replacement = "c0035fd6",
        )
    }
    file("lib/armeabi-v7a/$KEY_BOX") {
        replace(
            symbol = CHECK_FINGERPRINTS,
            fingerprint = "f0b503af",
            expected = "f0b503af",
            replacement = "01207047",
        )
    }
    file("lib/x86/$KEY_BOX") {
        replace(
            symbol = CHECK_FINGERPRINTS,
            fingerprint = "5589e553",
            expected = "5589e553",
            replacement = "b001c390",
        )
    }
    file("lib/x86_64/$KEY_BOX") {
        replace(
            symbol = CHECK_FINGERPRINTS,
            fingerprint = "554889e5",
            expected = "554889e5",
            replacement = "b001c390",
        )
    }
}

@Suppress("unused")
val fixKeyBoxPatch = patch(
    name = "Fix KeyBox initialization",
    description = "Allows CHZZK's bundled keys to load after patching and resigning the app.",
) {
    compatibleWith(COMPATIBILITY_CHZZK)
    dependsOn(fixKeyBoxNativePatch)
}
