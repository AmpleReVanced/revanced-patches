package app.revanced.patches.kakaomap.misc.extension

import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaomap.shared.Constants.COMPATIBILITY_KAKAO_MAP

val addExtensionPatch = bytecodePatch {
    compatibleWith(COMPATIBILITY_KAKAO_MAP)
    extendWith("extensions/kakaomap.mpe")
    dependsOn(sharedExtensionPatch)

    execute { /* NOP */ }
}