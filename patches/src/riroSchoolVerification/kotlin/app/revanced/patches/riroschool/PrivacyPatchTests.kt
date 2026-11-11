package app.revanced.patches.riroschool

import app.morphe.patcher.patch.Patch

import app.revanced.patches.riroschool.misc.analytics.*
import app.revanced.patches.riroschool.misc.integrity.*
import app.revanced.patches.riroschool.shared.*

/**
 * 역할: 독립 실행 가능한 패치 회귀 검사 전체를 실행한다.
 * 입력: 원본 APK와 디코딩한 manifest를 지정하는 RIRO_TEST_APK·RIRO_TEST_MANIFEST 환경 변수.
 * 출력: 계약 검사 통과 메시지 또는 실패 예외.
 */
fun main() {
    val publicPatches = listOf(disableFirebaseAnalyticsPatch, disableGoogleAnalyticsPatch,
        disableRirosoftAnalyticsPatch, disableVpnDetectionPatch, disableAdbDetectionPatch, spoofGmsAvailabilityPatch)
    check(publicPatches.map { it.name }.toSet().size == 6)
    check(!spoofGmsAvailabilityPatch.default && spoofGmsAvailabilityPatch.dependencies.isEmpty())
    publicPatches.forEach { checkDependencies(it, mutableSetOf()) }
    requireRiroMetadata("com.rirosoft.riroschool", "7.5.6", "179")
    for (target in listOf(
        Triple("other.app", "7.5.6", "179"),
        Triple("com.rirosoft.riroschool", "7.5.7", "179"),
        Triple("com.rirosoft.riroschool", "7.5.6", "180"),
    )) check(runCatching { requireRiroMetadata(target.first, target.second, target.third) }.isFailure)
    runDevicePatchTests()
    runIdentityPatchTests()
    runFirebasePatchTests()
    runGooglePatchTests()
    println("PASS: target, detector, identity, Firebase and ML Kit contracts")
}

/**
 * 역할: 정적 필드 초기화 순서로 생긴 null 의존성과 순환 의존성을 거부한다.
 * 입력: 공개 패치 또는 그 의존성과 현재 탐색 경로.
 * 출력: 정상 그래프이면 반환하고 불완전한 그래프이면 검사 실패.
 */
private fun checkDependencies(patch: Patch<*>, active: MutableSet<Patch<*>>) {
    check(active.add(patch)) { "Cyclic patch dependency" }
    patch.dependencies.forEach { dependency -> checkDependencies(requireNotNull(dependency), active) }
    active.remove(patch)
}
