package app.revanced.patches.unicorn.nativecode

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getReference
import app.revanced.patches.unicorn.misc.addExtensionPatch
import app.revanced.patches.unicorn.shared.Constants.COMPATIBILITY_UNICORN
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import java.security.MessageDigest
import java.util.Locale

private const val NATIVE_BRIDGE_CLASS = "Lapp/revanced/extension/unicorn/core/NativeBridge;"
private const val EXTENSION_PACKAGE = "Lapp/revanced/extension/unicorn/"

private val invokeOpcodes = setOf(
    Opcode.INVOKE_DIRECT,
    Opcode.INVOKE_DIRECT_RANGE,
    Opcode.INVOKE_INTERFACE,
    Opcode.INVOKE_INTERFACE_RANGE,
    Opcode.INVOKE_STATIC,
    Opcode.INVOKE_STATIC_RANGE,
    Opcode.INVOKE_SUPER,
    Opcode.INVOKE_SUPER_RANGE,
    Opcode.INVOKE_VIRTUAL,
    Opcode.INVOKE_VIRTUAL_RANGE,
)

private val implementedNativeReferences = setOf(
    "Lcom/unicornsoft/android/unicornpro/core/License\$Companion;->native_GetDeviceIds(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/License\$Companion;->native_GetExpiredAt(J)Ljava/lang/String;:false",
    "Lcom/unicornsoft/android/unicornpro/core/License\$Companion;->native_GetId(J)Ljava/lang/String;:false",
    "Lcom/unicornsoft/android/unicornpro/core/License\$Companion;->native_GetIsExpired(J)Z:false",
    "Lcom/unicornsoft/android/unicornpro/core/License\$Companion;->native_GetIsTrial(J)Z:false",
    "Lcom/unicornsoft/android/unicornpro/core/License\$Companion;->native_GetLife(J)I:false",
    "Lcom/unicornsoft/android/unicornpro/core/License\$Companion;->native_GetLocalizations(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/License\$Companion;->native_GetMaxDevice(J)I:false",
    "Lcom/unicornsoft/android/unicornpro/core/License\$Companion;->native_GetScope(J)Ljava/lang/String;:false",
    "Lcom/unicornsoft/android/unicornpro/core/License\$Companion;->native_delete(J)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Auth\$Companion;->native_GetEmail(J)Ljava/lang/String;:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Auth\$Companion;->native_GetId(J)Ljava/lang/String;:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Auth\$Companion;->native_GetScope(J)I:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Auth\$Companion;->native_delete(J)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Companion;->native_DetachLicense(JLcom/unicornsoft/android/unicornpro/core/CallbackV;Lcom/unicornsoft/android/unicornpro/core/CallbackL;)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Companion;->native_GetAuth(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Companion;->native_GetDevice(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Companion;->native_GetLicense(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Companion;->native_SignIn(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/unicornsoft/android/unicornpro/core/CallbackL;Lcom/unicornsoft/android/unicornpro/core/CallbackL;)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Companion;->native_StartAppProcess(JLcom/unicornsoft/android/unicornpro/core/CallbackL;Lcom/unicornsoft/android/unicornpro/core/CallbackL;)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Device\$Companion;->native_GetServerId(J)Ljava/lang/String;:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$Device\$Companion;->native_delete(J)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseTracker\$Companion;->native_Interrupt(J)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseTracker\$Companion;->native_delete(J)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseTracker\$Companion;->native_new(JJLcom/unicornsoft/android/unicornpro/core/CallbackV;Lcom/unicornsoft/android/unicornpro/core/CallbackV;)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$SignInRequireException\$Companion;->native_GetTypeId()J:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$UnconfirmedEmailException\$Companion;->native_GetTypeId()J:false",
    "Lcom/unicornsoft/android/unicornpro/core/LicenseManager\$UnconfirmedEmailException\$Companion;->native_GetUserId(J)Ljava/lang/String;:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/ExceptionPtr\$Companion;->native_GetTypeId(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/ExceptionPtr\$Companion;->native_delete(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/PairSS\$Companion;->native_GetFirst(J)Ljava/lang/String;:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/PairSS\$Companion;->native_GetSecond(J)Ljava/lang/String;:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/PairSS\$Companion;->native_delete(J)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS\$Companion;->native_GetBegin(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS\$Companion;->native_GetEnd(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS\$Companion;->native_delete(J)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS\$Iterator\$Companion;->native_Get(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS\$Iterator\$Companion;->native_GetNext(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS\$Iterator\$Companion;->native_IsEquals(JJ)Z:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS\$Iterator\$Companion;->native_delete(J)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS\$Companion;->native_GetBegin(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS\$Companion;->native_GetEnd(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS\$Companion;->native_delete(J)V:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS\$Iterator\$Companion;->native_Get(J)Ljava/lang/String;:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS\$Iterator\$Companion;->native_GetNext(J)J:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS\$Iterator\$Companion;->native_IsEquals(JJ)Z:false",
    "Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS\$Iterator\$Companion;->native_delete(J)V:false",
)

@Suppress("unused")
val unlockProFeaturesPatch = bytecodePatch(
    name = "Unlock Pro features",
    description = "Makes Unicorn Pro report an active Pro license while preserving the original native object graph.",
) {
    compatibleWith(COMPATIBILITY_UNICORN)
    dependsOn(addExtensionPatch)

    execute {
        classDefForEach { classDef ->
            if (classDef.type.startsWith(EXTENSION_PACKAGE)) return@classDefForEach

            val methods = classDef.methods.filter { method ->
                method.implementation?.instructions?.any { instruction ->
                    instruction.bridgeReferenceOrNull() != null
                } == true
            }

            if (methods.isEmpty()) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            methods.map(mutableClass::findMutableMethodOf).forEach { method ->
                method.instructions.forEachIndexed { index, instruction ->
                    val replacement = instruction.toBridgeInstructionOrNull() ?: return@forEachIndexed
                    method.replaceInstruction(index, replacement)
                }
            }
        }
    }
}

private fun Instruction.toBridgeInstructionOrNull(): BuilderInstruction? {
    val reference = bridgeReferenceOrNull() ?: return null
    val isStatic = opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE
    val bridgeReference = reference.toBridgeReference(isStatic)

    return when (this) {
        is BuilderInstruction35c -> BuilderInstruction35c(
            Opcode.INVOKE_STATIC,
            registerCount,
            registerC,
            registerD,
            registerE,
            registerF,
            registerG,
            bridgeReference,
        )

        is BuilderInstruction3rc -> BuilderInstruction3rc(
            Opcode.INVOKE_STATIC_RANGE,
            startRegister,
            registerCount,
            bridgeReference,
        )

        else -> null
    }
}

private fun Instruction.bridgeReferenceOrNull(): MethodReference? {
    if (opcode !in invokeOpcodes) return null

    val reference = getReference<MethodReference>() ?: return null
    return if (reference.signatureKey(false) in implementedNativeReferences) reference else null
}

private fun MethodReference.toBridgeReference(isStatic: Boolean): MethodReference {
    val bridgeParameterTypes = buildList {
        if (!isStatic) add("Ljava/lang/Object;")
        parameterTypes.forEach { add(it.toString().objectifiedType()) }
    }

    return ImmutableMethodReference(
        NATIVE_BRIDGE_CLASS,
        bridgeName(isStatic),
        bridgeParameterTypes,
        returnType,
    )
}

private fun MethodReference.bridgeName(isStatic: Boolean): String {
    val key = "$definingClass->$name(${parameterTypes.joinToString("")})$returnType:$isStatic"
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(key.toByteArray())
        .joinToString("") { String.format(Locale.US, "%02x", it.toInt() and 0xff) }

    return "m${hash.take(16)}"
}

private fun String.objectifiedType() =
    if (startsWith("L") || startsWith("[")) "Ljava/lang/Object;" else this

private fun MethodReference.signatureKey(isStatic: Boolean) =
    "$definingClass->$name(${parameterTypes.joinToString("")})$returnType:$isStatic"
