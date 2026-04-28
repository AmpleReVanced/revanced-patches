package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.util.findFreeRegister
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.chatlog.fingerprints.FlushToDBChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.InsertChatLogToChatRoomFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ProcessPendingOverwriteMessagesFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ReplaceToFeedFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private data class PendingOverwriteRetry(
    val managerAccessor: MethodReference,
    val processor: MethodReference,
) {
    fun instructions(register: Int) = """
        invoke-static {}, $managerAccessor
        move-result-object v$register
        invoke-static {v$register}, $processor
    """.trimIndent()
}

context(patchContext: BytecodePatchContext)
internal fun hookPendingOverwriteRetryAfterChatLogInsert(chatLog: ResolvedChatLogModel) {
    val retry = resolvePendingOverwriteRetry()
    val chatLogDaoClass = findChatLogDaoClass(chatLog)

    hookSyncMessageChatLogInsert(chatLog, chatLogDaoClass, retry)
    hookLocoChatRoomChatLogInsert(retry)
}

context(patchContext: BytecodePatchContext)
private fun resolvePendingOverwriteRetry(): PendingOverwriteRetry {
    val method = ProcessPendingOverwriteMessagesFingerprint.method
    val managerClass = method.definingClass.substringBefore('$') + ";"
    val matches = ProcessPendingOverwriteMessagesFingerprint.instructionMatches

    val managerAccessor = matches[0].instruction
        .getReference<MethodReference>()
        ?.takeIf { it.matchesNoArgReturn(managerClass) }
        ?: throw PatchException("Could not find pending overwrite manager accessor.")

    val processor = matches[2].instruction
        .getReference<MethodReference>()
        ?.takeIf { it.matchesSingleArgReturn(managerClass, "Z") }
        ?: throw PatchException("Could not find pending overwrite processor.")

    return PendingOverwriteRetry(managerAccessor, processor)
}

context(patchContext: BytecodePatchContext)
private fun findChatLogDaoClass(chatLog: ResolvedChatLogModel) =
    FlushToDBChatLogFingerprint.method.instructions.firstNotNullOfOrNull { instruction ->
        instruction
            .takeIf { it.opcode == Opcode.INVOKE_VIRTUAL }
            ?.getReference<MethodReference>()
            ?.takeIf { it.matchesChatLogDaoWrite(chatLog.chatLogType) }
            ?.definingClass
    } ?: throw PatchException("Could not find ChatLog DAO write method.")

context(patchContext: BytecodePatchContext)
private fun hookSyncMessageChatLogInsert(
    chatLog: ResolvedChatLogModel,
    chatLogDaoClass: String,
    retry: PendingOverwriteRetry,
) {
    val chatLogsManagerClass = patchContext.mutableClassDefBy(ReplaceToFeedFingerprint.classDef)
    val syncChatLogToDBMethod = chatLogsManagerClass.methods.firstOrNull { method ->
        method.parameterTypes == listOf(
            "J",
            chatLog.chatLogType,
            "Z",
            CONTINUATION_TYPE,
        ) &&
                method.returnType == OBJECT_TYPE &&
                method.instructions.any { instruction ->
                    instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                            instruction.getReference<MethodReference>()
                                ?.matchesSyncChatLogDaoWrite(chatLogDaoClass, chatLog.chatLogType) == true
                }
    } ?: throw PatchException("Could not find sync chat log DB write method.")

    val returnIndex = syncChatLogToDBMethod.instructions.indexOfLast { instruction ->
        instruction.opcode == Opcode.SGET_OBJECT &&
                instruction.getReference<FieldReference>()?.definingClass == KOTLIN_UNIT_TYPE
    }.takeIf { it >= 0 }
        ?: throw PatchException("Could not find sync chat log DB write return.")

    syncChatLogToDBMethod.addInstructions(
        returnIndex,
        retry.instructions(syncChatLogToDBMethod.findFreeRegister(returnIndex)),
    )
}

context(patchContext: BytecodePatchContext)
private fun hookLocoChatRoomChatLogInsert(retry: PendingOverwriteRetry) {
    val insertChatLogToChatRoomMethod = InsertChatLogToChatRoomFingerprint.method
    val returnIndex = InsertChatLogToChatRoomFingerprint.instructionMatches.last().index

    val returnRegister =
        (insertChatLogToChatRoomMethod.instructions[returnIndex] as OneRegisterInstruction).registerA
    val pendingRegister = insertChatLogToChatRoomMethod.findFreeRegister(returnIndex, returnRegister)

    insertChatLogToChatRoomMethod.addInstructions(
        returnIndex,
        retry.instructions(pendingRegister),
    )
}