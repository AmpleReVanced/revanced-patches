package app.revanced.patches.kakaotalk.interaction.send.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val INPUT_BOX_VIEW =
    "Lcom/kakao/talk/activity/chatroom/inputbox/view/InputBoxView;"

internal object SendButtonClickFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf(INPUT_BOX_VIEW),
    returnType = "Lkotlin/Unit;",
    filters = listOf(
        methodCall(
            parameters = listOf(),
            returnType = "V",
            opcode = Opcode.INVOKE_INTERFACE,
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "InputBoxView.kt" },
)

internal object SendButtonLongClickFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf(INPUT_BOX_VIEW),
    returnType = "Lkotlin/Unit;",
    filters = listOf(
        methodCall(
            parameters = listOf(),
            returnType = "Z",
            opcode = Opcode.INVOKE_INTERFACE,
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "InputBoxView.kt" },
)