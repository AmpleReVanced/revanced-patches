package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.chatlog.fingerprints.CanReactToChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogVFieldPutBooleanFingerprint
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val IS_MODIFIED_METHOD = "revanced_isDeletedOrHidden"
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

        val chatLogClass = ChatLogFingerprint.classDef
        val vFieldType = ChatLogVFieldPutBooleanFingerprint.classDef.type
        val vField = chatLogClass.fields.first { it.type == vFieldType }

        chatLogClass.methods.add(
            isDeletedOrHiddenMethod(
                definingClass = chatLogClass.type,
                vFieldReference = vField.smaliReference,
                vFieldType = vFieldType,
            )
        )

        CanReactToChatLogFingerprint(chatLogClass.type).method.apply {
            val freeRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $BLOCK_MODIFIED_MESSAGE_REACTION
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :morphe_original
                    invoke-virtual {p1}, ${chatLogClass.type}->$IS_MODIFIED_METHOD()Z
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

private fun isDeletedOrHiddenMethod(
    definingClass: String,
    vFieldReference: String,
    vFieldType: String,
): MutableMethod = ImmutableMethod(
    definingClass,
    IS_MODIFIED_METHOD,
    emptyList(),
    "Z",
    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
    null,
    null,
    MutableMethodImplementation(3),
).toMutable().apply {
    addInstructionsWithLabels(
        0,
        """
            iget-object v0, p0, $vFieldReference
            if-eqz v0, :revanced_unmodified
            invoke-virtual {v0}, $vFieldType->getDeleted()Z
            move-result v1
            if-nez v1, :revanced_modified
            invoke-virtual {v0}, $vFieldType->getHidden()Z
            move-result v1
            if-nez v1, :revanced_modified
            :revanced_unmodified
            const/4 v0, 0x0
            return v0
            :revanced_modified
            const/4 v0, 0x1
            return v0
        """.trimIndent(),
    )
}
