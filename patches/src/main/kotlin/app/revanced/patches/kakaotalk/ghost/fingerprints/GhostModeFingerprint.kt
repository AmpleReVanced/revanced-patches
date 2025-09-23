package app.revanced.patches.kakaotalk.ghost.fingerprints

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.fingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.sun.jdi.request.MethodExitRequest

internal val locoMethodClassFingerprint = fingerprint {
    accessFlags(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR)
    parameters()
    returns("V")
    strings(
        "NONE",
        "GETCONF",
        "CHECKIN",
        "BUYCS",
        "MINI",
        "DOWN",
        "SHORT",
        "POST"
    )
    opcodes(
        Opcode.NEW_INSTANCE,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.CONST_STRING,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.MOVE_OBJECT,
        Opcode.INVOKE_DIRECT_RANGE,
        Opcode.SPUT_OBJECT
    )
}

internal val actionJobMethodFingerprint: (ClassDef) -> Fingerprint = { classDef ->
    fingerprint {
        accessFlags(AccessFlags.PUBLIC)
        returns(classDef.toString())
        parameters()
        opcodes(
            Opcode.SGET_OBJECT,
            Opcode.RETURN_OBJECT
        )
        custom { method, classDef ->
            method.getInstruction(0).getReference<FieldReference>()?.name == "ACTION" && classDef.sourceFile == "ActionJob.kt"
        }
    }
}

internal val  sendCurrentActionFingerprint: (ClassDef) -> Fingerprint = { actionJobClassDef ->
    fingerprint {
        accessFlags(AccessFlags.PUBLIC)
        returns("Ljava/lang/Object;")
        strings("call to \'resume\' before \'invoke\' with coroutine")
        opcodes(
            Opcode.INSTANCE_OF,
            Opcode.IF_EQZ,
            Opcode.MOVE_OBJECT,
            Opcode.CHECK_CAST,
            Opcode.IGET,
            Opcode.CONST_HIGH16,
            Opcode.AND_INT,
            Opcode.IF_EQZ,
        )
        custom { method, classDef ->
            classDef.sourceFile == "Protocol.kt" &&
            method.instructions
                .filter { it.opcode == Opcode.CHECK_CAST }
                .map { it.getReference<TypeReference>()?.type }
                .contains(actionJobClassDef.toString())
        }
    }
}

internal val protocolSuccessFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Ljava/lang/String;")
    parameters()
    strings(
        "Success(data=",
        ")"
    )
    custom { method, classDef -> classDef.sourceFile == "ProtocolResult.kt" && method.name == "toString" }
}