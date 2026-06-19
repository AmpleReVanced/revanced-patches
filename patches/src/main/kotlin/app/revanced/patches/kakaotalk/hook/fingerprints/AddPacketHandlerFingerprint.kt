package app.revanced.patches.kakaotalk.hook.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object LocoProtocolResponseHookFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    custom = { method, classDef ->
        classDef.sourceFile == "Protocol.kt" &&
                method.parameterTypes.size == 2 &&
                method.parameterTypes[0].toString() == classDef.type &&
                method.returnType == method.parameterTypes[1].toString() &&
                method.implementation?.instructions?.any { instruction ->
                    val reference = instruction.getReference<MethodReference>()
                    instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.parameterTypes?.isEmpty() == true &&
                            reference.returnType == "S"
                } == true
    }
)

internal object LocoMethodClassFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "LocoMethod.kt" &&
                classDef.superclass == "Ljava/lang/Enum;" &&
                method.name == "valueOf" &&
                method.parameterTypes == listOf("Ljava/lang/String;") &&
                method.returnType == classDef.type
    }
)

internal object LocoReqBuilderConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    custom = { method, classDef ->
        classDef.sourceFile == "LocoReq.kt" &&
                method.parameterTypes.size == 3 &&
                method.parameterTypes[1].toString() == "I" &&
                method.parameterTypes[2].toString() == "S" &&
                classDef.fields.count { it.type == "Lkotlin/Lazy;" } == 2
    }
)

internal object LocoConnectionConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    custom = { method, classDef ->
        classDef.sourceFile == "LocoConnection.kt" &&
                method.parameterTypes.size == 8 &&
                classDef.fields.any { it.type == "Ljava/util/Map;" } &&
                method.implementation?.instructions?.any { instruction ->
                    val reference = instruction.getReference<MethodReference>()
                    instruction.opcode == Opcode.INVOKE_INTERFACE &&
                            reference?.definingClass == "Ljava/util/Map;" &&
                            reference.name == "put"
                } == true
    }
)
