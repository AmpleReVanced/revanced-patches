package app.revanced.patches.kakaotalk.layout.chatroom.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MentionComponentIsMultiChatFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("L"),
    returnType = "Z",
    filters = listOf(
        methodCall(
            definingClass = "this",
            parameters = listOf(),
            returnType = "Lcom/kakao/talk/activity/chatroom/inputbox/view/ChatRoomEditText;",
        ),
        methodCall(
            parameters = listOf("L", "Landroid/widget/EditText;"),
            returnType = "Z",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "MentionComponent.kt" }
)