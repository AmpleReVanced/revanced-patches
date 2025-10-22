package app.revanced.patches.kakaotalk.chatroom

import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatroom.fingerprints.limit300PlusBaseChatRoomFingerprint
import app.revanced.patches.kakaotalk.chatroom.fingerprints.limit300PlusOpenChatRoomFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22t

@Suppress("unused")
val remove300PlusLimitBaseChatRoomPatch = bytecodePatch(
    name = "Disable 300+ unread limit (BaseChatRoom)",
    description = "Always show the real unread count instead of '300+' in base chatroom list"
) {
    compatibleWith("com.kakao.talk"("25.9.0"))

    execute {
        val method = limit300PlusBaseChatRoomFingerprint.method

        val branches = method.instructions
            .filterIsInstance<BuilderInstruction22t>()
            .filter { it.opcode == Opcode.IF_LT }
            .toList()

        branches.forEach { iflt ->
            val idx = method.instructions.indexOf(iflt)
            val gotoInsn = BuilderInstruction10t(
                Opcode.GOTO,
                iflt.target
            )
            method.replaceInstruction(idx, gotoInsn)
        }
    }
}

@Suppress("unused")
val remove300PlusLimitOpenChatRoomPatch = bytecodePatch(
    name = "Disable 300+ unread limit (OpenChatRoom)",
    description = "Always show the real unread count instead of '300+' in open chatroom list"
) {
    compatibleWith("com.kakao.talk"("25.9.0"))

    execute {
        val method = limit300PlusOpenChatRoomFingerprint.method

        method.instructions
            .filterIsInstance<BuilderInstruction22t>()
            .filter { it.opcode == Opcode.IF_LT }
            .toList()
            .forEach { iflt ->
                val idx = method.instructions.indexOf(iflt)
                val gotoInsn = BuilderInstruction10t(Opcode.GOTO, iflt.target)
                method.replaceInstruction(idx, gotoInsn)
            }
    }
}
