package app.revanced.patches.kakaotalk.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_KAKAO = Compatibility(
        name = "Kakao Talk",
        packageName = "com.kakao.talk",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFEE500, // Icon color in Morphe Manager
        signatures = setOf(
            "2b06cc3d47782d7c497c07f17cb5f859cd6bbcb66829f3e67b96b7a44820d2ce"
        ),
        targets = listOf(
            // "version = null" means the patch works with the latest app target
            // and is expected to work with all future app targets
            AppTarget(
                version = "26.6.1"
            )
        )
    )
}
