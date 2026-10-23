package app.revanced.patches.kakaotalk.misc.hook.fingerprints

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
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
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
    superclass = "Ljava/lang/Enum;",
    name = "valueOf",
    parameters = listOf("Ljava/lang/String;"),
    custom = { method, classDef ->
        classDef.sourceFile == "LocoMethod.kt" &&
            method.returnType == classDef.type
    }
)

internal object LocoReqBuilderConstructorFingerprint : Fingerprint(
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("L", "I", "S"),
    returnType = "V",
    custom = { _, classDef ->
        classDef.sourceFile == "LocoReq.kt" &&
            classDef.fields.count { it.type == "Lkotlin/Lazy;" } == 2
    }
)

internal object LocoConnectionConstructorFingerprint : Fingerprint(
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Ljava/util/Map;",
            name = "put",
            parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
            returnType = "Ljava/lang/Object;",
            opcode = Opcode.INVOKE_INTERFACE,
        ),
    ),
    custom = { _, classDef ->
        classDef.sourceFile == "LocoConnection.kt" &&
            classDef.fields.any { it.type == "Ljava/util/Map;" }
    }
)
