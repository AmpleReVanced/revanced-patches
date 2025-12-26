package app.revanced.patches.kakaotalk.chatroom

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatroom.fingerprints.getUnreadCountFingerprint
import app.revanced.patches.kakaotalk.chatroom.fingerprints.limit300PlusBaseChatRoomFingerprint
import app.revanced.patches.kakaotalk.chatroom.fingerprints.limit300PlusOpenChatRoomFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22t

@Suppress("unused")
val remove300PlusLimitChatRoomPatch = bytecodePatch(
    name = "Disable 300+ unread limit",
    description = "Always show the real unread count instead of '300+' in chatroom list"
) {
    compatibleWith("com.kakao.talk"("25.11.1"))

    execute {
        limit300PlusBaseChatRoomFingerprint.method.apply {
            val branches = instructions
                .filterIsInstance<BuilderInstruction22t>()
                .filter { it.opcode == Opcode.IF_LT }
                .toList()

            branches.forEach { iflt ->
                val idx = instructions.indexOf(iflt)
                val gotoInsn = BuilderInstruction10t(
                    Opcode.GOTO,
                    iflt.target
                )
                replaceInstruction(idx, gotoInsn)
            }
        }

        limit300PlusOpenChatRoomFingerprint.method.apply {
            instructions
                .filterIsInstance<BuilderInstruction22t>()
                .filter { it.opcode == Opcode.IF_LT }
                .toList()
                .forEach { iflt ->
                    val idx = instructions.indexOf(iflt)
                    val gotoInsn = BuilderInstruction10t(Opcode.GOTO, iflt.target)
                    replaceInstruction(idx, gotoInsn)
                }
        }

        getUnreadCountFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-virtual {p0}, $definingClass->a()I
                    move-result v0
                    return v0
                """.trimIndent()
            )
        }
    }
}