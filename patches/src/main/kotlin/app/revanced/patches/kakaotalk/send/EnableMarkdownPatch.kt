package app.revanced.patches.kakaotalk.send

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.send.fingerprints.EnableMarkdownFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val enableMarkdownPatch = bytecodePatch(
    name = "Enable Markdown",
    description = "Render plain text messages with markdown-style formatting.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch)

    execute {
        val method = EnableMarkdownFingerprint.method
        val jsonInvokeIndex = method.instructions.indexOfFirst { instruction ->
            if (instruction.opcode != Opcode.INVOKE_STATIC) return@indexOfFirst false

            val reference = instruction.getReference<MethodReference>() ?: return@indexOfFirst false
            reference.returnType == "Lorg/json/JSONObject;" &&
                    reference.parameterTypes == listOf(
                        "Lorg/json/JSONObject;",
                        "Lcom/kakao/talk/chat/ChatMessage;",
                        "Z"
                    )
        }
        if (jsonInvokeIndex < 0) {
            throw PatchException("Could not find markdown JSON builder call in x2")
        }

        val jsonMoveResultIndex = jsonInvokeIndex + 1
        if (jsonMoveResultIndex >= method.instructions.size || method.instructions[jsonMoveResultIndex].opcode != Opcode.MOVE_RESULT_OBJECT) {
            throw PatchException("Could not find JSON move-result after markdown JSON builder call")
        }
        val jsonRegister = (method.getInstruction(jsonMoveResultIndex) as OneRegisterInstruction).registerA

        val chatMessageInvokeIndex = method.instructions.indexOfFirst { instruction ->
            if (instruction.opcode != Opcode.INVOKE_STATIC) return@indexOfFirst false

            val reference = instruction.getReference<MethodReference>() ?: return@indexOfFirst false
            reference.returnType == "Lcom/kakao/talk/chat/ChatMessage;"
        }
        if (chatMessageInvokeIndex < 0) {
            throw PatchException("Could not find ChatMessage creation call in x2")
        }

        val chatMessageMoveResultIndex = chatMessageInvokeIndex + 1
        if (chatMessageMoveResultIndex >= method.instructions.size || method.instructions[chatMessageMoveResultIndex].opcode != Opcode.MOVE_RESULT_OBJECT) {
            throw PatchException("Could not find ChatMessage move-result after creation call")
        }
        val chatMessageRegister = (method.getInstruction(chatMessageMoveResultIndex) as OneRegisterInstruction).registerA

        val messageMethodReference = method.instructions
            .mapNotNull { instruction ->
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@mapNotNull null
                instruction.getReference<MethodReference>()
            }
            .firstOrNull {
                it.definingClass == "Lcom/kakao/talk/chat/ChatMessage;" &&
                        it.returnType == "Ljava/lang/CharSequence;" &&
                        it.parameterTypes.isEmpty()
            } ?: throw PatchException("Could not find ChatMessage text accessor in x2")

        val freeRegisters = method.getFreeRegisterProvider(
            jsonMoveResultIndex + 1,
            2,
            jsonRegister,
            chatMessageRegister
        )
        val scratchRegister = freeRegisters.getFreeRegister()
        val flagRegister = freeRegisters.getFreeRegister()

        method.addInstructionsWithLabels(
            jsonMoveResultIndex + 1,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->enableMarkdown()Z
                move-result v$flagRegister
                if-eqz v$flagRegister, :morphe_skip_markdown
                invoke-virtual {v$chatMessageRegister}, $messageMethodReference
                move-result-object v$scratchRegister
                invoke-static {v$scratchRegister}, Lkotlin/text/StringsKt__StringsKt;->A0(Ljava/lang/CharSequence;)Z
                move-result v$flagRegister
                if-nez v$flagRegister, :morphe_skip_markdown
                const-string v$scratchRegister, "markdown"
                const/4 v$flagRegister, 0x1
                invoke-virtual {v$jsonRegister, v$scratchRegister, v$flagRegister}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;
                :morphe_skip_markdown
                nop
            """.trimIndent()
        )
    }
}
