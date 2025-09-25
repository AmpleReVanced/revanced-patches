package app.revanced.patches.kakaotalk.chatroom

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatroom.fingerprints.checkMentionableFingerprint
import app.revanced.patches.kakaotalk.chatroom.fingerprints.isMultiChatFingerprint
import app.revanced.patches.kakaotalk.chatroom.fingerprints.limitMentionToNonMultiChatFingerprint
import app.revanced.patches.kakaotalk.chatroom.fingerprints.mentionComponentClassFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

/**
 * TODO: Not working at 25.8.0, needs rework
 */
@Suppress("unused")
val bypassInputMentionPatch = bytecodePatch(
    name = "Bypass input mention limit in non-multichat",
    description = "Bypass the limit of input mentions in non-multichat rooms",
) {
    compatibleWith("com.kakao.talk"("25.8.0"))

    execute {
        val mentionComponent = mentionComponentClassFingerprint.method.definingClass
        limitMentionToNonMultiChatFingerprint.method.instructions
            .indexOfFirst { it.opcode == Opcode.NEW_INSTANCE && (it as Instruction21c).getReference<TypeReference>()?.type == mentionComponent }
            .takeIf { it >= 0 }
            ?.let { index ->
                limitMentionToNonMultiChatFingerprint.method.removeInstruction(index - 1)
            }

        val isMultiChatMethod = isMultiChatFingerprint.method

        checkMentionableFingerprint.method.instructions
            .indexOfFirst { it.opcode == Opcode.INVOKE_STATIC
                    && it.getReference<MethodReference>()?.definingClass == isMultiChatMethod.definingClass
                    && it.getReference<MethodReference>()?.name == isMultiChatMethod.name }
            .takeIf { it >= 0 }
            ?.let { index ->
                val register = (checkMentionableFingerprint.method.getInstruction(index + 1) as Instruction11x).registerA
                checkMentionableFingerprint.method.addInstruction(
                    index + 2,
                    BuilderInstruction11n(
                        Opcode.CONST_4,
                        register,
                        0x1
                    )
                )
            }
    }
}