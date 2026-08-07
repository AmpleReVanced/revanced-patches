package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.chatlog.fingerprints.CanReactToChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogFingerprint
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/BlockModifiedMessageReactionPatch;"
private const val BLOCK_MODIFIED_MESSAGE_REACTION =
    "Lapp/revanced/extension/kakaotalk/settings/Settings;->blockModifiedMessageReaction()Z"

@Suppress("unused")
val blockModifiedMessageReactionPatch = bytecodePatch(
    name = "Block reactions on deleted or hidden messages",
    description = "Stops reactions, including the double tap gesture, from being sent on messages " +
            "that the server considers deleted or hidden and that are only still visible because " +
            "they are kept by a patch.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch, showDeletedHiddenOrEditedMessagePatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_block_modified_message_reaction",
                titleKey = "morphe_settings_patch_block_modified_message_reaction",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val chatLogType = ChatLogFingerprint.classDef.type

        CanReactToChatLogFingerprint(chatLogType).method.apply {
            val freeRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $BLOCK_MODIFIED_MESSAGE_REACTION
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :morphe_original
                    invoke-virtual {p1}, $chatLogType->$MODIFIED_MESSAGE_IS_DELETED_OR_HIDDEN_METHOD()Z
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :morphe_original
                    const/4 v$freeRegister, 0x0
                    return v$freeRegister
                    :morphe_original
                    nop
                """.trimIndent(),
            )
        }
    }
}
