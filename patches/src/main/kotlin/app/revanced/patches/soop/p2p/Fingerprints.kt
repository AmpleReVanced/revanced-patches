package app.revanced.patches.soop.p2p

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object PlaybackNativeConfigurationFingerprint : Fingerprint(
    custom = { method, _ ->
        method.hasNativeSetterCall("setSDKConfig", listOf("Ljava/lang/String;")) &&
            method.hasNativeSetterCall("setLowLatency", listOf("Z")) &&
            method.hasNativeSetterCall("setFixedParent", listOf("Z", "J", "I"))
    }
)

internal object HlsEngineV2OffFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("HLS_ENGINE_V2_ANDROID_OFF")
)

internal object ColonyConnectFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("connect Colony in", "connect Colony out")
)

internal object P2PCommandDispatchFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, _ ->
        val instructions = method.implementation?.instructions

        instructions != null &&
            method.parameterTypes.size == 1 &&
            instructions.count { it.opcode == Opcode.INSTANCE_OF } >= 4 &&
            instructions.count { instruction ->
                instruction.getReference<MethodReference>()?.matchesMethod(
                    name = "build",
                    parameterTypes = emptyList(),
                    returnType = "Lcom/google/protobuf/GeneratedMessageLite;"
                ) == true
            } >= 3 &&
            instructions.any { instruction ->
                instruction.getReference<MethodReference>()?.matchesMethod(
                    name = "toByteString",
                    parameterTypes = emptyList(),
                    returnType = "Lcom/google/protobuf/ByteString;"
                ) == true
            }
    }
)

internal fun MethodReference.matchesMethod(
    name: String,
    parameterTypes: List<String>,
    returnType: String,
) = this.name == name &&
    this.returnType == returnType &&
    this.parameterTypes.map(CharSequence::toString) == parameterTypes

private fun Method.hasNativeSetterCall(
    name: String,
    parameterTypes: List<String>,
) = implementation?.instructions?.any { instruction ->
    instruction.getReference<MethodReference>()?.matchesMethod(name, parameterTypes, "V") == true
} == true