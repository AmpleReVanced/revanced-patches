package app.revanced.patches.starnote.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_STARNOTE = Compatibility(
        name = "StarNote",
        packageName = "com.onyx.galaxy.global.note",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x3D8AFF,
        targets = listOf(
            AppTarget(
                version = "1.4.3",
                isExperimental = true
            ),
        ),
    )
}