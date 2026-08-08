package app.revanced.patches.kakaotalk.integrity

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.integrity.fingerprints.InstallSourceFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

private const val PLAY_STORE_PACKAGE_NAME = "com.android.vending"

@Suppress("unused")
val spoofInstallerPackageNamePatch = bytecodePatch(
    name = "Spoof installer package name",
    description = "Reports Google Play as the install source in the integrity report, which " +
            "otherwise reveals a sideloaded install.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        InstallSourceFingerprint.method.returnEarly(PLAY_STORE_PACKAGE_NAME)
    }
}