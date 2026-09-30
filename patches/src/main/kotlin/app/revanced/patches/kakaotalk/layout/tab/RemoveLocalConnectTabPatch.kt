package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val removeLocalConnectTabPatch = bytecodePatch(
    name = "Remove local connect tab",
    description = "Removes the local connect tab from the bottom navigation bar.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        removeMainTab("YP_TAB")
    }
}
