package app.revanced.patches.soop.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_SOOP = Compatibility(
        name = "SOOP",
        packageName = "kr.co.nowcom.mobile.afreeca",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x0068FF,
        targets = listOf(
            AppTarget(
                version = "8.25.2"
            )
        )
    )
}