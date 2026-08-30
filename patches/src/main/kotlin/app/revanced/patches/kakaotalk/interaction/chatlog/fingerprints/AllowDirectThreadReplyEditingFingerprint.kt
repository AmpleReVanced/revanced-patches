package app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private object ModifyMessageActionClassFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    returnType = "Ljava/lang/String;",
    filters = listOf(fieldAccess(name = "modify", type = "I", opcode = Opcode.SGET)),
    custom = { _, classDef -> classDef.sourceFile == "RealAction.kt" },
)

internal object ModifyMessageActionConstructorFingerprint : Fingerprint(
    classFingerprint = ModifyMessageActionClassFingerprint,
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("L", "L"),
    returnType = "V",
    filters = listOf(
        methodCall(
            parameters = listOf("L", "L"),
            returnType = "Z",
            opcode = Opcode.INVOKE_STATIC,
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
    ),
)