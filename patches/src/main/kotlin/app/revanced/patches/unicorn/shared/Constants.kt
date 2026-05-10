package app.revanced.patches.unicorn.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_UNICORN = Compatibility(
        name = "Unicorn Pro",
        packageName = "com.unicornsoft.android.unicornpro",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x6147FF,
        targets = listOf(
            AppTarget(
                version = "1.30.447"
            )
        )
    )
}
