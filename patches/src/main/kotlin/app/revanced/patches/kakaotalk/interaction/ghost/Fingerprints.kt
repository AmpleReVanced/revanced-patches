package app.revanced.patches.kakaotalk.interaction.ghost

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal object LocoMethodClassFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(),
    returnType = "V",
    strings = listOf(
        "NONE",
        "GETCONF",
        "CHECKIN",
        "BUYCS",
        "MINI",
        "DOWN",
        "POST"
    ),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.NEW_INSTANCE,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.CONST_STRING,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.INVOKE_DIRECT_RANGE,
        Opcode.SPUT_OBJECT
    )
)

internal val actionJobMethodFingerprint: (ClassDef) -> Fingerprint = { classDef ->
    Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC),
        returnType = classDef.toString(),
        parameters = listOf(),
        filters = OpcodesFilter.opcodesToFilters(
            Opcode.SGET_OBJECT,
            Opcode.RETURN_OBJECT
        ),
        custom = { method, classDef ->
            method.getInstruction(0)
                .getReference<FieldReference>()?.name == "ACTION" && classDef.sourceFile == "ActionJob.kt"
        }
    )
}

internal val sendCurrentActionFingerprint: (ClassDef) -> Fingerprint = { actionJobClassDef ->
    Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC),
        returnType = "Ljava/lang/Object;",
        strings = listOf("call to \'resume\' before \'invoke\' with coroutine"),
        filters = OpcodesFilter.opcodesToFilters(
            Opcode.INSTANCE_OF,
            Opcode.IF_EQZ,
            Opcode.MOVE_OBJECT,
            Opcode.CHECK_CAST,
            Opcode.IGET,
            Opcode.CONST_HIGH16,
            Opcode.AND_INT,
            Opcode.IF_EQZ,
        ),
        custom = { method, classDef ->
            classDef.sourceFile == "Protocol.kt" &&
                    method.instructions
                        .filter { it.opcode == Opcode.CHECK_CAST }
                        .map { it.getReference<TypeReference>()?.type }
                        .contains(actionJobClassDef.toString())
        }
    )
}

internal object ProtocolSuccessFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf(
        "Success(data=",
        ")"
    ),
    custom = { method, classDef -> classDef.sourceFile == "ProtocolResult.kt" && method.name == "toString" }
)

private object ChatOnRoomJobFingerprint : Fingerprint(
    filters = listOf(string("chatId"), string("token"), string("opt")),
    custom = { _, classDef -> classDef.sourceFile == "ChatOnRoomJob.kt" },
)

internal object ChatOnRoomRequestFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(newInstance(type = { ChatOnRoomJobFingerprint.classDef.type })),
    custom = { _, classDef -> classDef.sourceFile == "Protocol.kt" },
)

internal class LocoBodyFlagFingerprint(vararg keys: String) : Fingerprint(
    superclass = "Lcom/kakao/talk/core/loco/protocol/job/LocoJob;",
    filters = listOf(
        anyInstruction(*keys.map(::string).toTypedArray()),
        methodCall(
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/Object;"),
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object ReadPointerFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("L", "J"),
    filters = listOf(string("last_read_log_id")),
    custom = { _, classDef -> classDef.sourceFile == "ChatRoom.kt" },
)

internal val ReadPointerFingerprint.readLogId
    get() = method.instructions.first { it.opcode == Opcode.IPUT_WIDE }.getReference<FieldReference>()!!

internal object SentChatLogFingerprint : Fingerprint(
    classFingerprint = ReadPointerFingerprint,
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(parameters = listOf("J", "Ljava/lang/Long;", "I"), returnType = "Z"),
        methodCall(reference = { ReadPointerFingerprint.method }),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal object ReceivedChatLogFingerprint : Fingerprint(
    classFingerprint = ReadPointerFingerprint,
    returnType = "L",
    parameters = listOf("L", "Z", "L", "Z"),
    filters = listOf(methodCall(reference = { ReadPointerFingerprint.method })),
)

internal object MessageUnreadCountFingerprint : Fingerprint(
    returnType = "I",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT),
        fieldAccess(opcode = Opcode.SGET_OBJECT, name = "YOURS", location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_WIDE),
        fieldAccess(opcode = Opcode.IGET_WIDE, location = MatchAfterImmediately()),
        methodCall(parameters = listOf("J", "J"), returnType = "I", location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogRecyclerItem.kt" },
)
