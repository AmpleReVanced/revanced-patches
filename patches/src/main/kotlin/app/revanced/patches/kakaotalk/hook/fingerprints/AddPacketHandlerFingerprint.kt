package app.revanced.patches.kakaotalk.hook.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object KakaoApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/talk/application/App;",
    name = "onCreate",
    returnType = "V",
    parameters = emptyList(),
)

internal object LocoProtocolResponseHookFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    filters = listOf(
        methodCall(
            parameters = emptyList(),
            returnType = "S",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "Protocol.kt" &&
            method.parameterTypes.size == 2 &&
            method.parameterTypes[0].toString() == classDef.type &&
            method.returnType == method.parameterTypes[1].toString()
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
    filters = listOf(
        methodCall(
            definingClass = "Ljava/util/Map;",
            name = "put",
            opcode = Opcode.INVOKE_INTERFACE,
        ),
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "LocoConnection.kt" &&
            method.parameterTypes.size == 8 &&
            classDef.fields.any { it.type == "Ljava/util/Map;" }
    }
)
