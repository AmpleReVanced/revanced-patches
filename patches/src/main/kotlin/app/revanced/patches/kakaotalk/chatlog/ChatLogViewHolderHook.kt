package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogItemViewHolderFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogViewHolderSetupChatInfoViewFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

context(patchContext: BytecodePatchContext)
internal fun hookChatInfoViewHolderFlags(chatLog: ResolvedChatLogModel) {
    val setupChatInfoViewMethod = ChatLogViewHolderSetupChatInfoViewFingerprint.method
    val getChatLogItemMethod = ChatLogItemViewHolderFingerprint.method

    val setModifyIndex = setupChatInfoViewMethod.instructions.indexOfFirst {
        it.opcode == Opcode.INVOKE_VIRTUAL &&
                it.getReference<MethodReference>()?.name == "setModify"
    }.takeIf { it >= 0 }
        ?: throw PatchException("Could not find ChatInfoView setModify call.")

    setupChatInfoViewMethod.addInstructionsWithLabels(
        setModifyIndex + 1,
        """
            invoke-virtual {v0}, $CHAT_INFO_VIEW_CLASS->getExtension()$CHAT_INFO_EXTENSION_CLASS
            move-result-object v5
            if-eqz v5, :skip_set_flags

            invoke-virtual {p0}, $getChatLogItemMethod
            move-result-object v6
            instance-of v7, v6, ${chatLog.chatLogType}
            if-eqz v7, :cond_chatlog_null
            check-cast v6, ${chatLog.chatLogType}
            goto :goto_chatlog_cvar
            :cond_chatlog_null
            const/4 v6, 0x0
            :goto_chatlog_cvar
            if-nez v6, :cond_get_vfield
            const/4 v8, 0x0
            const/4 v9, 0x0
            goto :goto_set_flags

            :cond_get_vfield
            iget-object v7, v6, ${chatLog.chatLogType}->${chatLog.vFieldFieldName}:${chatLog.vFieldType}

            invoke-virtual {v7}, ${chatLog.vFieldType}->${ChatLogFlag.Deleted.getMethodName}()Z
            move-result v8

            invoke-virtual {v7}, ${chatLog.vFieldType}->${ChatLogFlag.Hidden.getMethodName}()Z
            move-result v9

            :goto_set_flags
            invoke-virtual {v5, v8}, $CHAT_INFO_EXTENSION_CLASS->${ChatLogFlag.Deleted.extensionSetterName}(Z)V
            invoke-virtual {v5, v9}, $CHAT_INFO_EXTENSION_CLASS->${ChatLogFlag.Hidden.extensionSetterName}(Z)V

            :skip_set_flags
            nop
        """.trimIndent(),
    )
}