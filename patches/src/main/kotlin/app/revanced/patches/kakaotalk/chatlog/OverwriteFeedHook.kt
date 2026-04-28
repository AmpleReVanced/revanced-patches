package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ReplaceToFeedFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private data class OverwriteFeedBranch(
    val name: String,
    val flag: ChatLogFlag,
    val indices: List<Int>,
    val required: Boolean = true,
)

context(patchContext: BytecodePatchContext)
internal fun hookDirectOverwriteFeeds(chatLog: ResolvedChatLogModel) {
    val replaceToFeedMethod = ReplaceToFeedFingerprint.method

    val branches = listOf(
        OverwriteFeedBranch(
            name = "DELETE_TO_ALL",
            flag = ChatLogFlag.Deleted,
            indices = listOf(ReplaceToFeedFingerprint.instructionMatches.first().index),
        ),
        OverwriteFeedBranch(
            name = "Feed",
            flag = ChatLogFlag.Hidden,
            indices = replaceToFeedMethod.indicesOfFeedTypeField("Feed"),
        ),
        OverwriteFeedBranch(
            name = "TIMECHAT_SAFE_BOT_BLIND",
            flag = ChatLogFlag.Hidden,
            indices = listOfNotNull(replaceToFeedMethod.indexOfTimeChatSafeBotBlindOrNull(chatLog)),
            required = false,
        ),
    )

    branches.forEach { branch ->
        if (branch.indices.isEmpty() && branch.required) {
            throw PatchException("Could not find ${branch.name} branch in ChatLogsManager.")
        }
    }

    branches.flatMap { branch -> branch.indices.map { index -> branch to index } }
        .sortedByDescending { (_, index) -> index }
        .forEach { (branch, index) ->
            replaceToFeedMethod.replaceOverwriteBranchWithFlag(index, branch.flag, chatLog)
        }
}

private fun MutableMethod.indicesOfFeedTypeField(fieldName: String) =
    instructions.mapIndexedNotNull { index, instruction ->
        index.takeIf {
            instruction.opcode == Opcode.SGET_OBJECT &&
                    instruction.getReference<FieldReference>()?.name == fieldName
        }
    }

private fun MutableMethod.indexOfTimeChatSafeBotBlindOrNull(chatLog: ResolvedChatLogModel) =
    instructions.indexOfFirst {
        it.opcode == Opcode.INVOKE_VIRTUAL_RANGE &&
                it.getReference<MethodReference>()?.let { reference ->
                    reference.definingClass == definingClass &&
                            reference.parameterTypes == listOf(chatLog.chatLogType, "J", "J") &&
                            reference.returnType == "V"
                } == true
    }.takeIf { it >= 0 }

private fun MutableMethod.replaceOverwriteBranchWithFlag(
    index: Int,
    flag: ChatLogFlag,
    chatLog: ResolvedChatLogModel,
) {
    replaceInstruction(index, "nop")
    addInstructions(
        index + 1,
        chatLog.setFlagAndFlushInstructions(definingClass, flag),
    )
}