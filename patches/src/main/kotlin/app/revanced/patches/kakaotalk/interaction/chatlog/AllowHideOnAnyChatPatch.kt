package app.revanced.patches.kakaotalk.interaction.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.CheckIsAllowedHideFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction

@Suppress("unused")
val allowHideOnAnyChatPatch = patch(
    name = "Allow Hide on Any Chat",
    description = "Users with hiding privileges can hide any chat, including their own messages.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        CheckIsAllowedHideFingerprint.method.apply {
            val index = indexOfFirstInstructionOrThrow(Opcode.CMP_LONG)
            val register = getInstruction<ThreeRegisterInstruction>(index).registerA
            addInstruction(index + 1, "const/4 v$register, 0x1")
        }
    }
}