package app.revanced.patches.riroschool.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.PackageMetadata

internal val riroCompatibility = Compatibility(
    packageName = "com.rirosoft.riroschool", name = "Riro School", description = null,
    apkFileType = null, appIconColor = null as Int?, signatures = null,
    targets = listOf(AppTarget(version = "7.5.6")),
)

/** Reject unsupported package/version metadata, including forced patch application. Only the verified 7.5.6/179 target is accepted. */
internal fun requireRiroTarget(packageMetadata: PackageMetadata) = requireRiroMetadata(
    packageMetadata.packageName, packageMetadata.versionName, packageMetadata.versionCode,
)

/** Validate the app ID, version name and version code before any transformation. */
internal fun requireRiroMetadata(packageName: String, versionName: String, versionCode: String) {
    require(packageName == "com.rirosoft.riroschool" && versionName == "7.5.6" && versionCode == "179") {
        "Unsupported target: $packageName $versionName/$versionCode; expected com.rirosoft.riroschool 7.5.6/179"
    }
}
