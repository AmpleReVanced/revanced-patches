package app.revanced.patches.flexcil.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_FLEXCIL = Compatibility(
        name = "Flexcil",
        packageName = "com.flexcil.flexcilnote",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x0096FA,
        targets = listOf(
            AppTarget(
                version = "1.4.3.30"
            )
        )
    )
}
