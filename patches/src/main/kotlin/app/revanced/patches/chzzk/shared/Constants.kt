package app.revanced.patches.chzzk.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_CHZZK = Compatibility(
        name = "Chzzk",
        packageName = "com.navercorp.game.android.community",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x00FFA3,
        targets = listOf(
            AppTarget(
                version = "3.6.0"
            )
        )
    )
}