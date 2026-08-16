package app.revanced.patches.samsungkeyboard.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_SAMSUNG_KEYBOARD = Compatibility(
        name = "Samsung Keyboard",
        packageName = "com.samsung.android.honeyboard",
        apkFileType = ApkFileType.APK,
        targets = listOf(
            AppTarget(
                version = "5.9.40.48",
                versionCode = 594048300,
                minSdk = 33,
            ),
        ),
    )
}