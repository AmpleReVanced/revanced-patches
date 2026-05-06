package app.revanced.patches.dcinside.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_DC_INSIDE = Compatibility(
        name = "dcinside",
        packageName = "com.dcinside.app.android",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x3D4491, // Icon color in Morphe Manager
        targets = listOf(
            // "version = null" means the patch works with the latest app target
            // and is expected to work with all future app targets
            AppTarget(
                version = "5.2.9"
            )
        )
    )
}
