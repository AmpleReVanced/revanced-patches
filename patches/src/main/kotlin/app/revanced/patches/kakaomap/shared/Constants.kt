package app.revanced.patches.kakaomap.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_KAKAO_MAP = Compatibility(
        name = "KakaoMap",
        packageName = "net.daum.android.map",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0xFEE500,
        targets = listOf(
            AppTarget(
                version = "6.28.1",
                isExperimental = true
            ),
        ),
    )
}