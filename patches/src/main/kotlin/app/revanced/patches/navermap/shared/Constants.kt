package app.revanced.patches.navermap.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_NAVER_MAP = Compatibility(
        name = "NAVER Map",
        packageName = "com.nhn.android.nmap",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x03C75A,
        targets = listOf(
            AppTarget(
                version = "6.10.0.16",
                isExperimental = true
            ),
            AppTarget(
                version = "6.9.1.3",
                isExperimental = true
            ),
        ),
    )
}