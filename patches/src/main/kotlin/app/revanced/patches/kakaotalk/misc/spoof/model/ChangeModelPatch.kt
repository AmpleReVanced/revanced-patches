package app.revanced.patches.kakaotalk.misc.spoof.model

import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.build.BuildInfo
import app.revanced.patches.all.misc.build.baseSpoofBuildInfoPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

val spoofBuildInfoPatch = baseSpoofBuildInfoPatch {
    BuildInfo(
        brand = "samsung",
        manufacturer = "samsung",
        device = "qssi",
        product = "gts9wifixx",
        model = "SM-X710",
        board = "kalama",
        hardware = "qcom",
        cpuAbi = "arm64-v8a",
        socManufacturer = "QTI",
        socModel = "SM8550",
        fingerprint = "samsung/gts9wifixx/qssi:14/UP1A.231005.007/X710XXU5BYA1:user/release-keys",
        // Keep the standalone Build fields consistent with the values encoded in the fingerprint,
        // so a server cross-checking Build.ID/TYPE/TAGS against the fingerprint sees no mismatch.
        id = "UP1A.231005.007",
        display = "UP1A.231005.007.X710XXU5BYA1",
        type = "user",
        tags = "release-keys",
    )
}

@Suppress("unused")
val changeModelPatch = bytecodePatch(
    name = "Change model",
    description = "Changes the device model to supporting subdevice features",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(spoofBuildInfoPatch)

    execute {
        // NOP
    }
}