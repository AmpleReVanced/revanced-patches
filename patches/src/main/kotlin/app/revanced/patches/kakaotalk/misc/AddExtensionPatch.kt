package app.revanced.patches.kakaotalk.misc

import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val addExtensionPatch = bytecodePatch(
    name = "Add extension",
    description = "Adds extension support to the app.",
) {
    compatibleWith("com.kakao.talk"("25.11.0"))
    extendWith("extensions/kakaotalk.rve")
    dependsOn(sharedExtensionPatch)

    execute { /* NOP */ }
}