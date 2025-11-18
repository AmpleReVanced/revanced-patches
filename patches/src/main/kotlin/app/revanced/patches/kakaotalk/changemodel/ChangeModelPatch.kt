package app.revanced.patches.kakaotalk.changemodel

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.build.BuildInfo
import app.revanced.patches.all.misc.build.baseSpoofBuildInfoPatch

val spoofBuildInfoPatch = baseSpoofBuildInfoPatch {
    BuildInfo(
        brand = "samsung",
        manufacturer = "samsung",
        device = "qssi",
        product = "gts9wifixx",
        model = "SM-X710",
        fingerprint = "samsung/gts9wifixx/qssi:14/UP1A.231005.007/X710XXU5BYA1:user/release-keys",
    )
}

@Suppress("unused")
val changeModelPatch = bytecodePatch(
    name = "Change model",
    description = "Changes the device model to supporting subdevice features",
) {
    compatibleWith("com.kakao.talk"("25.10.0"))
    dependsOn(spoofBuildInfoPatch)

    execute {
        // NOP
    }
}