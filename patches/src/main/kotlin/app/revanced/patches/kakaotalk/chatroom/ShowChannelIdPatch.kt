package app.revanced.patches.kakaotalk.chatroom

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.chatroom.fingerprints.ChatRoomProfileEditBindFingerprint
import app.revanced.patches.kakaotalk.chatroom.fingerprints.ChatRoomSetIdFingerprint
import app.revanced.patches.kakaotalk.chatroom.fingerprints.ChatRoomSideInitFingerprint
import app.revanced.patches.kakaotalk.chatroom.fingerprints.ChatRoomSideTitleItemLambdaFingerprint
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.addKakaoTalkResources
import app.revanced.util.localRegisterCount
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val CHANNEL_ID_EXTENSION = "Lapp/revanced/extension/kakaotalk/chatroom/ChannelIdInfoExtension;"

@Suppress("unused")
val showChannelIdPatch = bytecodePatch(
    name = "Show chatroom channel ID",
    description = "Shows the channel ID in chatroom settings and copies it when tapping the chat side title.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch, addResourcesPatch, sharedExtensionPatch)

    execute {
        addKakaoTalkResources()
        addChatSideTitleCopy()
        addChatRoomSettingsChannelId()
    }
}

private fun BytecodePatchContext.addChatSideTitleCopy() {
    val initMethod = ChatRoomSideInitFingerprint.method
    val getChatIdIndex = initMethod.instructions.indexOfFirst { instruction ->
        val reference = instruction.getReference<MethodReference>()
        instruction.opcode == Opcode.INVOKE_VIRTUAL &&
            reference != null &&
            reference.definingClass == "Landroid/content/Intent;" &&
            reference.name == "getLongExtra" &&
            reference.returnType == "J"
    }
    if (getChatIdIndex < 0) {
        throw PatchException("Could not find chat side chatId read.")
    }

    val chatIdRegister = (initMethod.instructions.getOrNull(getChatIdIndex + 1) as? OneRegisterInstruction)
        ?.takeIf { it.opcode == Opcode.MOVE_RESULT_WIDE }
        ?.registerA
        ?: throw PatchException("Could not infer chat side chatId register.")

    initMethod.addInstructions(
        getChatIdIndex + 2,
        """
            invoke-static {p0, v$chatIdRegister, v${chatIdRegister + 1}}, $CHANNEL_ID_EXTENSION->setCurrentChannelId(Landroid/app/Activity;J)V
        """.trimIndent(),
    )

    val titleLambdaMethod = ChatRoomSideTitleItemLambdaFingerprint.method
    val titleRenderIndex = titleLambdaMethod.instructions.indexOfFirst { instruction ->
        val reference = instruction.getReference<MethodReference>()
        instruction.opcode == Opcode.INVOKE_INTERFACE &&
            reference != null &&
            reference.definingClass == "Lkotlin/jvm/functions/Function4;" &&
            reference.name == "invoke"
    }
    if (titleRenderIndex < 0) {
        throw PatchException("Could not find chat side title renderer invocation.")
    }

    val titleRenderInstruction = titleLambdaMethod.instructions[titleRenderIndex] as? FiveRegisterInstruction
        ?: throw PatchException("Could not inspect chat side title renderer registers.")
    val modifierRegister = titleRenderInstruction.registerD

    titleLambdaMethod.addInstructions(
        titleRenderIndex,
        """
            invoke-static {v$modifierRegister}, $CHANNEL_ID_EXTENSION->makeCopyableTitleModifier(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object v$modifierRegister
            check-cast v$modifierRegister, Landroidx/compose/ui/d;
        """.trimIndent(),
    )
}

private fun BytecodePatchContext.addChatRoomSettingsChannelId() {
    val chatRoomIdField = ChatRoomSetIdFingerprint.method.instructions
        .firstNotNullOfOrNull { instruction ->
            instruction.takeIf { it.opcode == Opcode.IPUT_WIDE }
                ?.getReference<FieldReference>()
        }
        ?: throw PatchException("Could not infer ChatRoom id field.")

    val chatRoomIdGetter = ChatRoomSetIdFingerprint.classDef.methods
        .firstOrNull { method ->
            method.parameterTypes.isEmpty() &&
                method.returnType == "J" &&
                method.instructions.count { it.opcode != Opcode.NOP } == 2 &&
                method.instructions.any { it.opcode == Opcode.RETURN_WIDE } &&
                method.instructions.any { instruction ->
                    instruction.opcode == Opcode.IGET_WIDE &&
                        instruction.getReference<FieldReference>()?.sameField(chatRoomIdField) == true
                }
        }
        ?: throw PatchException("Could not infer ChatRoom id getter.")

    val bindMethod = ChatRoomProfileEditBindFingerprint.method
    val chatRoomType = ChatRoomSetIdFingerprint.classDef.type
    val chatRoomRegister = bindMethod.instructions.withIndex()
        .firstNotNullOfOrNull { (index, instruction) ->
            val reference = instruction.getReference<MethodReference>()
            if (reference?.returnType != chatRoomType) return@firstNotNullOfOrNull null

            (bindMethod.instructions.getOrNull(index + 1) as? OneRegisterInstruction)
                ?.takeIf { it.opcode == Opcode.MOVE_RESULT_OBJECT }
                ?.registerA
        }
        ?: throw PatchException("Could not infer ChatRoom register in profile bind method.")

    val rootStringIndex = bindMethod.instructions.indexOfFirst { instruction ->
        instruction.opcode == Opcode.CONST_STRING &&
            instruction.getReference<StringReference>()?.string == "getRoot(...)"
    }
    if (rootStringIndex < 1) {
        throw PatchException("Could not find profile root insertion point.")
    }

    val rootRegister = (bindMethod.instructions[rootStringIndex - 1] as? OneRegisterInstruction)
        ?.takeIf { it.opcode == Opcode.MOVE_RESULT_OBJECT }
        ?.registerA
        ?: throw PatchException("Could not infer profile root register.")

    val temporaryWideRegister = bindMethod.localRegisterCount - 2
    if (temporaryWideRegister < 0 ||
        temporaryWideRegister == chatRoomRegister ||
        temporaryWideRegister + 1 == chatRoomRegister ||
        temporaryWideRegister == rootRegister ||
        temporaryWideRegister + 1 == rootRegister
    ) {
        throw PatchException("Could not reserve temporary registers for channel ID display.")
    }

    bindMethod.addInstructions(
        rootStringIndex,
        """
            invoke-virtual {v$chatRoomRegister}, ${chatRoomIdGetter.definingClass}->${chatRoomIdGetter.name}()J
            move-result-wide v$temporaryWideRegister
            invoke-static {v$rootRegister, v$temporaryWideRegister, v${temporaryWideRegister + 1}}, $CHANNEL_ID_EXTENSION->bind(Landroid/view/View;J)V
        """.trimIndent(),
    )
}

private fun FieldReference.sameField(other: FieldReference) =
    definingClass == other.definingClass &&
        name == other.name &&
        type == other.type
