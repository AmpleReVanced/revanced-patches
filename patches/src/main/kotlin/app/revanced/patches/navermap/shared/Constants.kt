package app.revanced.patches.navermap.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_NAVER_MAP = Compatibility(
        name = "NAVER Map",
        packageName = "com.nhn.android.nmap",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x03C75A,
        targets = listOf(
            AppTarget(
                version = "6.8.0.5",
                isExperimental = true
            ),
        ),
    )
}