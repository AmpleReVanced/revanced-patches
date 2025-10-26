package app.revanced.patches.kakaotalk.integrity

import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val spoofApkChecksumsPatch = bytecodePatch(
    name = "Spoof apk checksums",
    description = "Spoofs the apk checksums to pass integrity checks.",
) {
    compatibleWith("com.kakao.talk"("25.9.0"))

    execute {

    }
}