package app.revanced.patches.kakaotalk.feature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/DisableOpenChatRoomCommentPatch;"

@Suppress("unused")
val disableOpenChatRoomCommentPatch = bytecodePatch(
    name = "Disable open chat room comments",
    description = "Adds a setting to disable comments in open chat rooms.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_open_chat_room_comment_disabled",
                titleKey = "morphe_settings_patch_open_chat_room_comment_disabled",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)
    }
}
