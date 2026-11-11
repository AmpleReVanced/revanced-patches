package app.revanced.patches.riroschool.misc.integrity

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.revanced.patches.riroschool.shared.requireRiroTarget
import app.revanced.patches.riroschool.shared.riroCompatibility

/**
 * 역할: 진단 수집 차단과 독립적으로 앱 시작 시 GMS 가용성만 SUCCESS로 처리한다.
 * 입력: 검증된 리로스쿨 7.5.6/179 APK와 사용자의 명시적인 패치 선택.
 * 출력: IntroActivity.b의 앱 전용 가용성 판정만 변경하며 실제 GMS·모듈은 유지한다.
 */
val spoofGmsAvailabilityPatch = bytecodePatch(
    name = "Spoof GMS availability",
    description = "Treat the app startup Google Play Services availability check as SUCCESS. Does not install GMS or change analytics collection.",
    default = false,
) {
    compatibleWith(riroCompatibility)
    execute {
        requireRiroTarget(packageMetadata)
        spoofGmsAvailability()
    }
}
