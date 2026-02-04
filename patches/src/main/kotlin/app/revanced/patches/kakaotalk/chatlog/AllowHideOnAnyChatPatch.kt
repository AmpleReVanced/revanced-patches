package app.revanced.patches.kakaotalk.chatlog

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatlog.fingerprints.checkIsAllowedHideFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.checkIsEqualWithMyUserIdFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val allowHideOnAnyChatPatch = bytecodePatch(
    name = "Allow Hide on Any Chat",
    description = "Users with hiding privileges can hide any chat, including their own messages.",
) {
    compatibleWith("com.kakao.talk"("26.1.2"))

    execute {
        val checkIsAllowedHideMethod = checkIsAllowedHideFingerprint.method
        val checkIsEqualWithMyUserIdClass = checkIsEqualWithMyUserIdFingerprint.classDef

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