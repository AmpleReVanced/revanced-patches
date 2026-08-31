package app.revanced.patches.kakaotalk.interaction.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.CheckIsAllowedHideFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.CheckIsEqualWithMyUserIdFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val allowHideOnAnyChatPatch = bytecodePatch(
    name = "Allow Hide on Any Chat",
    description = "Users with hiding privileges can hide any chat, including their own messages.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val isMyUserIdMethod = CheckIsEqualWithMyUserIdFingerprint.originalMethod
        CheckIsAllowedHideFingerprint.method.apply {
            val index = indexOfFirstInstructionOrThrow(
                methodCall(
                    name = isMyUserIdMethod.name,
                    parameters = listOf("J"),
                    returnType = "Z",
                    opcode = Opcode.INVOKE_INTERFACE,
                ),
            )
            val register = getInstruction<OneRegisterInstruction>(index + 1).registerA
            addInstruction(index + 2, "const/4 v$register, 0x0")
        }
    }
}