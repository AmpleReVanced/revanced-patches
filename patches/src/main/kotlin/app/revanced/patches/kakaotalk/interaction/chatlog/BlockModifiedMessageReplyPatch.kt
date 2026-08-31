package app.revanced.patches.kakaotalk.interaction.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ReplyCommentSendFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.SwipeReplyMovementFlagsFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.parameterTypeNames
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/BlockModifiedMessageReplyPatch;"
private const val BLOCK_MODIFIED_MESSAGE_REPLY =
    "Lapp/revanced/extension/kakaotalk/settings/Settings;->blockModifiedMessageReply()Z"

@Suppress("unused")
val blockModifiedMessageReplyPatch = bytecodePatch(
    name = "Block replies on deleted or hidden messages",
    description = "Stops the swipe gesture, and any reply or comment send, from targeting messages " +
            "that the server considers deleted or hidden and that are only still visible because " +
            "they are kept by a patch.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch, showDeletedHiddenOrEditedMessagePatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_block_modified_message_reply",
                titleKey = "morphe_settings_patch_block_modified_message_reply",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val chatLogType = ChatLogFingerprint.classDef.type

        val movementFlagsMethod = SwipeReplyMovementFlagsFingerprint.method
        val callbackClass = SwipeReplyMovementFlagsFingerprint.classDef

        val canSwipeMethod = callbackClass.methods.singleOrNull { method ->
            method.parameterTypeNames.size == 2 &&
                    method.parameterTypeNames[1] == chatLogType &&
                    method.returnType == "Z"
        } ?: throw PatchException("Could not find the swipe-to-reply predicate.")

        val invokeIndex = movementFlagsMethod.indexOfFirstInstructionOrThrow {
            getReference<MethodReference>()?.smaliReference == canSwipeMethod.smaliReference
        }

        val invokeInstruction = movementFlagsMethod.getInstruction<Instruction35c>(invokeIndex)
        if (invokeInstruction.registerCount != 3) {
            throw PatchException("Unexpected swipe predicate call shape.")
        }
        val chatLogRegister = invokeInstruction.registerE

        val resultIndex = invokeIndex + 1
        if (movementFlagsMethod.getInstruction(resultIndex).opcode != Opcode.MOVE_RESULT) {
            throw PatchException("Swipe predicate result is not captured.")
        }
        val resultRegister = movementFlagsMethod.getInstruction<OneRegisterInstruction>(resultIndex).registerA

        // If the compiler routed the predicate's result into the chat log's register, the read below
        // would hit a boolean; fail loudly instead of emitting a method that cannot verify.
        if (resultRegister == chatLogRegister) {
            throw PatchException("Swipe predicate reuses the chat log register for its result.")
        }

        val freeRegister = movementFlagsMethod
            .getFreeRegisterProvider(resultIndex + 1, 1, chatLogRegister, resultRegister)
            .getFreeRegister4Bit()

        movementFlagsMethod.addInstructionsWithLabels(
            resultIndex + 1,
            """
                invoke-static {}, $BLOCK_MODIFIED_MESSAGE_REPLY
                move-result v$freeRegister
                if-eqz v$freeRegister, :morphe_keep_swipe
                invoke-virtual {v$chatLogRegister}, $chatLogType->$MODIFIED_MESSAGE_IS_DELETED_OR_HIDDEN_METHOD()Z
                move-result v$freeRegister
                if-eqz v$freeRegister, :morphe_keep_swipe
                const/16 v$resultRegister, 0x0
                :morphe_keep_swipe
                nop
            """.trimIndent(),
        )

        // Backstop for the menu and reply-bar entries the swipe gate misses. Reply and comment both
        // reach this composer with the source as p1; a null source is a normal message.
        ReplyCommentSendFingerprint.method.apply {
            val freeRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $BLOCK_MODIFIED_MESSAGE_REPLY
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :morphe_send
                    move-object/from16 v$freeRegister, p1
                    if-eqz v$freeRegister, :morphe_send
                    invoke-virtual {v$freeRegister}, $chatLogType->$MODIFIED_MESSAGE_IS_DELETED_OR_HIDDEN_METHOD()Z
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :morphe_send
                    sget-object v$freeRegister, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                    return-object v$freeRegister
                    :morphe_send
                    nop
                """.trimIndent(),
            )
        }
    }
}
