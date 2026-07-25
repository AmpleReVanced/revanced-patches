package app.revanced.patches.soop.p2p

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getMutableMethod
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import app.revanced.patches.shared.misc.native.nativePatch
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP
import app.revanced.util.argumentRegister
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val P2P_DISABLED_SDK_CONFIG =
    """{"HLS_ENGINE_V2_ANDROID_OFF":true,"LL_ANDROID_OFF":true,"LL_HLS_OFF":true}"""

private val disableNativeP2PPatch = nativePatch(COMPATIBILITY_SOOP) {
    // _ParentConnect: force the P2P-vs-direct branch to direct. `????` = version-volatile bytes.
    file("lib/arm64-v8a/libstreamer.so") {
        forceBranch(
            fingerprint = "690a40b9 ???????? 1f2003d5 ???????? 08011e32 e00316aa e1058052 220f8052",
            offset = 4,
        )
    }
}

@Suppress("unused")
val disableP2PPatch = bytecodePatch(
    name = "Disable P2P",
    description = "Disables SOOP peer-assisted grid streaming.",
) {
    compatibleWith(COMPATIBILITY_SOOP)
    dependsOn(disableNativeP2PPatch)

    execute {
        HlsEngineV2OffFingerprint.method.returnEarly(true)
        ColonyConnectFingerprint.method.returnEarly()
        P2PCommandDispatchFingerprint.method.returnEarly()

        PlaybackNativeConfigurationFingerprint.method.apply {
            findGetterBeforeSetter(
                setterName = "setSDKConfig",
                setterParameterTypes = listOf("Ljava/lang/String;"),
                getterReturnType = "Ljava/lang/String;",
            ).getMutableMethod().returnString(P2P_DISABLED_SDK_CONFIG)

            findGetterBeforeSetter(
                setterName = "setLowLatency",
                setterParameterTypes = listOf("Z"),
                getterReturnType = "Z",
            ).getMutableMethod().returnEarly(false)

            findGetterBeforeSetter(
                setterName = "setFixedParent",
                setterParameterTypes = listOf("Z", "J", "I"),
                getterReturnType = "Z",
            ).getMutableMethod().returnEarly(false)
        }
    }
}

private fun MutableMethod.findGetterBeforeSetter(
    setterName: String,
    setterParameterTypes: List<String>,
    getterReturnType: String,
): MethodReference {
    val instructions = implementation?.instructions?.toList()
        ?: throw PatchException("Could not inspect playback configuration method.")

    val setterIndex = instructions.indexOfFirst { instruction ->
        instruction.getReference<MethodReference>()?.matchesMethod(setterName, setterParameterTypes, "V") == true
    }

    if (setterIndex < 0) {
        throw PatchException("Could not find playback native setter: $setterName.")
    }

    val valueRegister = instructions[setterIndex].argumentRegister(0)

    for (index in setterIndex - 1 downTo 1) {
        val instruction = instructions[index]
        if ((instruction.opcode != Opcode.MOVE_RESULT && instruction.opcode != Opcode.MOVE_RESULT_OBJECT) ||
            (instruction as OneRegisterInstruction).registerA != valueRegister
        ) {
            continue
        }

        val getter = instructions[index - 1].getReference<MethodReference>()
            ?: throw PatchException("Could not resolve getter feeding playback native setter: $setterName.")
        if (getter.returnType != getterReturnType || getter.parameterTypes.isNotEmpty()) {
            throw PatchException("Unexpected getter feeding playback native setter: $setterName.")
        }

        return getter
    }

    throw PatchException("Could not find getter feeding playback native setter: $setterName.")
}

private fun MutableMethod.returnString(value: String) {
    replaceInstructions(
        0,
        """
            const-string v0, "${value.toSmaliStringLiteral()}"
            return-object v0
        """.trimIndent(),
    )
}

private fun String.toSmaliStringLiteral() =
    replace("\\", "\\\\")
        .replace("\"", "\\\"")