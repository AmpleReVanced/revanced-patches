package app.revanced.patches.kakaotalk.interaction.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.classDefByOrNull
import app.morphe.patcher.patch.classDefForEach
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogContextMenuActionsFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogItemViewHolderFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogViewHolderSetupChatInfoViewFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.chatLogAdapterBindFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.senderlessMessageMenuFingerprint
import app.revanced.util.parameterRegister
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/revanced/extension/kakaotalk/chatlog/details/MessageDetailsExtension;"
private const val HOLDER_INTERFACE = "Lapp/revanced/extension/kakaotalk/chatlog/details/MessageDetailsViewHolder;"

internal fun addMessageDetailsMenus(chatLogType: String) {
    val holder = ChatLogItemViewHolderFingerprint.classDef
    val messageField = ChatLogItemViewHolderFingerprint.originalMethod.instructions
        .first().getReference<FieldReference>()!!
    holder.interfaces.add(HOLDER_INTERFACE)
    holder.methods.add(
        bridgeMethod(
            definingClass = holder.type,
            name = "getMessageDetailsSource",
            returnType = "Ljava/lang/Object;",
            registerCount = 2,
            instructions = """
                iget-object v0, p0, $messageField
                return-object v0
            """,
        ),
    )

    val itemView = classDefBy(holder.superclass!!).instanceFields.single { it.type == "Landroid/view/View;" }
    chatLogAdapterBindFingerprint(holder.type).method.apply {
        val index = indexOfFirstInstructionOrThrow(Opcode.RETURN_VOID)
        val register = getFreeRegisterProvider(index, 1, parameterRegister(0)).getFreeRegister4Bit()
        addInstructionsWithLabels(
            index,
            """
                instance-of v$register, p1, ${ChatLogViewHolderSetupChatInfoViewFingerprint.originalClassDef.type}
                if-nez v$register, :bound
                iget-object v$register, p1, ${itemView.smaliReference}
                invoke-static {v$register, p1}, $EXTENSION_CLASS->bindFallbackMenu(Landroid/view/View;$HOLDER_INTERFACE)V
            """,
            ExternalLabel("bound", getInstruction(index)),
        )
    }

    senderlessMessageMenuFingerprint(chatLogType).apply {
        val index = instructionMatches.last().index
        val registers = method.getFreeRegisterProvider(index, 2)
        val owner = registers.getFreeRegister4Bit()
        val message = registers.getFreeRegister4Bit()
        val source = classDef.instanceFields.single { it.type == chatLogType }
        val activity = classDef.instanceFields.single { it.type == "Landroidx/fragment/app/FragmentActivity;" }
        method.addInstructions(
            index,
            """
                move-object/from16 v$owner, p0
                iget-object v$message, v$owner, ${source.smaliReference}
                iget-object v$owner, v$owner, ${activity.smaliReference}
                invoke-static {v$owner, v$message}, $EXTENSION_CLASS->showMenu(Landroid/content/Context;Ljava/lang/Object;)Z
            """,
        )
    }

    val managerType = ChatLogContextMenuActionsFingerprint.originalClassDef.instanceFields
        .single { classDefByOrNull(it.type)?.sourceFile == "ChatLogContextMenuManager.kt" }.type
    classDefForEach { classDef ->
        val longClick = classDef.methods.singleOrNull {
            it.name == "onLongClick" && it.parameterTypes == listOf("Landroid/view/View;") && it.returnType == "Z"
        } ?: return@classDefForEach
        if (!generateSequence(classDef.superclass) { classDefByOrNull(it)?.superclass }.any { it == holder.type }) {
            return@classDefForEach
        }
        val calls = longClick.instructions.mapNotNull { it.getReference<MethodReference>() }
        if (calls.any { it.definingClass == holder.type && it.name == "onLongClick" }) return@classDefForEach
        val method = mutableClassDefBy(classDef.type).methods.single { it.name == "onLongClick" }
        val index = if (calls.any { it.definingClass == managerType }) {
            method.indexOfFirstInstructionOrThrow(Opcode.IF_NEZ)
        } else {
            0
        }
        val register = method.getFreeRegisterProvider(index, 1).getFreeRegister4Bit()
        val guard = if (index == 0) "" else {
            val condition = method.getInstruction<OneRegisterInstruction>(index).registerA
            "if-eqz v$condition, :original"
        }
        method.addInstructionsWithLabels(
            index,
            """
                $guard
                invoke-static {p1, p0}, $EXTENSION_CLASS->showMenu(Landroid/view/View;$HOLDER_INTERFACE)Z
                move-result v$register
                if-eqz v$register, :original
                return v$register
            """,
            ExternalLabel("original", method.getInstruction(index)),
        )
    }
}