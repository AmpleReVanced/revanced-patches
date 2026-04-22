package app.revanced.patches.kakaotalk.feature

import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableOpenChatRoomCommentPatch = bytecodePatch(
    name = "Disable open chat room comments",
    description = "Adds a setting to disable comments in open chat rooms.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch, overrideFeatureFlagPatch)

    execute {}
}
