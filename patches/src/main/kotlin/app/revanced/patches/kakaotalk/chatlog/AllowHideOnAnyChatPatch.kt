package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.chatlog.fingerprints.CheckIsAllowedHideFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.CheckIsEqualWithMyUserIdFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val allowHideOnAnyChatPatch = bytecodePatch(
    name = "Allow Hide on Any Chat",
    description = "Users with hiding privileges can hide any chat, including their own messages.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val checkIsAllowedHideMethod = CheckIsAllowedHideFingerprint.method
        val checkIsEqualWithMyUserIdClass = CheckIsEqualWithMyUserIdFingerprint.classDef

        val index = checkIsAllowedHideMethod.instructions
            .indexOfFirst { it.opcode == Opcode.INVOKE_INTERFACE &&
                    it.getReference<MethodReference>()?.definingClass?.contains(checkIsEqualWithMyUserIdClass.toString().split("/")[0]) == true }

        val moveResult = checkIsAllowedHideMethod.getInstruction(index + 1) as OneRegisterInstruction
        checkIsAllowedHideMethod.addInstruction(
            index + 2,
            BuilderInstruction11n(
                Opcode.CONST_4,
                moveResult.registerA,
                0x0
            )
        )
    }
}