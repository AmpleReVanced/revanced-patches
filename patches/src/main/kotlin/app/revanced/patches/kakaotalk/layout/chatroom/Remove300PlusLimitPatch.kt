package app.revanced.patches.kakaotalk.layout.chatroom

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.layout.chatroom.fingerprints.GetUnreadCountFingerprint
import app.revanced.patches.kakaotalk.layout.chatroom.fingerprints.Limit300PlusBaseChatRoomFingerprint
import app.revanced.patches.kakaotalk.layout.chatroom.fingerprints.Limit300PlusOpenChatRoomFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22t
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction

@Suppress("unused")
val remove300PlusLimitChatRoomPatch = bytecodePatch(
    name = "Disable 300+ unread limit",
    description = "Always show the real unread count instead of '300+' in chatroom list"
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        Limit300PlusBaseChatRoomFingerprint.method.apply {
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

        Limit300PlusOpenChatRoomFingerprint.method.apply {
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

        GetUnreadCountFingerprint.method.apply {
            val clampIndex = instructions.indexOfFirst {
                it.opcode == Opcode.CONST_16 &&
                        (it as? NarrowLiteralInstruction)?.narrowLiteral == 300
            }.takeIf { it >= 0 }
                ?: throw PatchException("Could not find 300 unread clamp")

            removeInstructions(clampIndex, 3)
        }
    }
}
