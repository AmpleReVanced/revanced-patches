package app.revanced.patches.riroschool.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.PackageMetadata

internal val riroCompatibility = Compatibility(
    packageName = "com.rirosoft.riroschool", name = "Riro School", description = null,
    apkFileType = null, appIconColor = null as Int?, signatures = null,
    targets = listOf(AppTarget(version = "7.5.6")),
)

/**
 * 역할: 강제 적용을 포함하여 검증하지 않은 앱과 버전의 패치를 거부한다.
 * 입력: 현재 Morphe 패치 세션의 패키지와 버전 정보.
 * 출력: 승인된 7.5.6/179이면 반환하고 그 외에는 예외를 발생시킨다.
 */
internal fun requireRiroTarget(packageMetadata: PackageMetadata) = requireRiroMetadata(
    packageMetadata.packageName, packageMetadata.versionName, packageMetadata.versionCode,
)

/**
 * 역할: 대상 메타데이터를 비교하는 공통 검증을 수행한다.
 * 입력: 앱 ID, versionName, 문자열 versionCode.
 * 출력: 일치하면 반환하고 일치하지 않으면 변경 전에 실패한다.
 */
internal fun requireRiroMetadata(packageName: String, versionName: String, versionCode: String) {
    require(packageName == "com.rirosoft.riroschool" && versionName == "7.5.6" && versionCode == "179") {
        "Unsupported target: $packageName $versionName/$versionCode; expected com.rirosoft.riroschool 7.5.6/179"
    }
}
