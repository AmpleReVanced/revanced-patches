package app.revanced.patches.kakaotalk.interaction.chatlog

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val MOTION_EVENT = "Landroid/view/MotionEvent;"
private const val RECYCLER_VIEW = "Landroidx/recyclerview/widget/RecyclerView;"
private const val DETAILS = "Lapp/revanced/extension/kakaotalk/chatlog/details/MessageDetailsExtension;"

private object ChatTouchDispatchFingerprint : Fingerprint(
    name = "dispatchTouchEvent",
    parameters = listOf(MOTION_EVENT),
    returnType = "Z",
    filters = listOf(methodCall("Landroid/view/GestureDetector;->onTouchEvent(${MOTION_EVENT})Z")),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogRecyclerView.kt" },
)

private fun chatSwipeTouchFingerprint(sourceFile: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L", "L", RECYCLER_VIEW, "Landroid/view/View;", MOTION_EVENT),
    returnType = "Z",
    filters = listOf(
        methodCall("$MOTION_EVENT->getAction()I"),
        literal(3),
        opcode(Opcode.IF_EQ),
        methodCall("$MOTION_EVENT->getAction()I"),
        fieldAccess(type = "Z", opcode = Opcode.IPUT_BOOLEAN),
        fieldAccess(type = "Z", opcode = Opcode.IGET_BOOLEAN),
        methodCall(name = "getBindingAdapterPosition", returnType = "I"),
        fieldAccess(type = "Z", opcode = Opcode.IGET_BOOLEAN),
        fieldAccess(type = "Z", opcode = Opcode.IGET_BOOLEAN),
        methodCall("Landroid/view/View;->getContext()Landroid/content/Context;"),
    ),
    custom = { _, classDef -> classDef.sourceFile == sourceFile },
)

internal fun preserveChatSwipeGestures() {
    ChatTouchDispatchFingerprint.method.addInstruction(
        0, "invoke-static {p0, p1}, $DETAILS->trackSystemBackGesture(Landroid/view/View;$MOTION_EVENT)V",
    )

    for (source in listOf("ChatLogItemTouchHelperCallback.kt", "ChatThreadChatLogItemTouchHelperCallback.kt")) {
        chatSwipeTouchFingerprint(source).apply {
            val cancel = instructionMatches[2].getInstruction<TwoRegisterInstruction>()
            val index = instructionMatches[4].index + 1
            val reply = instructionMatches[5].fieldReference
            val oppositeSwipe = instructionMatches[8].fieldReference
            method.addInstructionsWithLabels(
                index,
                """
                    if-ne v${cancel.registerA}, v${cancel.registerB}, :resume
                    const/4 v${cancel.registerA}, 0x0
                    iput-boolean v${cancel.registerA}, p0, $reply
                    iput-boolean v${cancel.registerA}, p0, $oppositeSwipe
                    return v${cancel.registerA}
                """,
                ExternalLabel("resume", method.getInstruction(index)),
            )

            classDef.methods.single { it.name == "getMovementFlags" }.apply {
                val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {p1}, $DETAILS->isSystemBackGesture(Landroid/view/View;)Z
                        move-result v$register
                        if-eqz v$register, :resume
                        const/4 v$register, 0x0
                        return v$register
                    """,
                    ExternalLabel("resume", getInstruction(0)),
                )
            }
        }
    }
}