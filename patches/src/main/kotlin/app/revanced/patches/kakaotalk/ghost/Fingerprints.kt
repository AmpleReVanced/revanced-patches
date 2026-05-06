package app.revanced.patches.kakaotalk.ghost

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.fingerprint
import app.morphe.util.getReference
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
        "SHORT",
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