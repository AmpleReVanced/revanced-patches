package app.revanced.util

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

internal fun Method.hasMethodCall(
    definingClass: String,
    name: String,
    returnType: String? = null,
): Boolean = implementation?.instructions?.any { instruction ->
    val reference = instruction.getReference<MethodReference>() ?: return@any false
    reference.definingClass == definingClass &&
            reference.name == name &&
            (returnType == null || reference.returnType == returnType)
} == true

internal fun Method.hasFieldReference(
    definingClass: String,
    name: String,
): Boolean = implementation?.instructions?.any { instruction ->
    val reference = instruction.getReference<FieldReference>() ?: return@any false
    reference.definingClass == definingClass && reference.name == name
} == true

internal val FieldReference.smaliReference: String
    get() = "$definingClass->$name:$type"

internal val Field.smaliReference: String
    get() = "$definingClass->$name:$type"

internal val MethodReference.smaliReference: String
    get() = "$definingClass->$name(${parameterTypeNames.joinToString("")})$returnType"

internal val MethodReference.parameterTypeNames: List<String>
    get() = parameterTypes.map { it.toString() }

internal val Method.parameterTypeNames: List<String>
    get() = parameterTypes.map { it.toString() }

internal val String.registerWidth: Int
    get() = if (this == "J" || this == "D") 2 else 1

internal fun Map<String, ClassDef>.requireClass(type: String): ClassDef =
    this[type] ?: throw PatchException("Could not resolve class $type.")

internal fun MethodReference.matches(method: Method): Boolean =
    MethodUtil.methodSignaturesMatch(this, method)

internal fun MethodReference.matches(reference: MethodReference): Boolean =
    definingClass == reference.definingClass &&
        MethodUtil.methodSignaturesMatch(this, reference)

internal val MutableMethod.localRegisterCount: Int
    get() {
        val implementation = implementation
            ?: throw PatchException("Could not inspect registers for $definingClass->$name.")
        val receiverWidth = if (AccessFlags.STATIC.isSet(accessFlags)) 0 else 1
        val parameterWidth = parameterTypeNames.sumOf { it.registerWidth }
        return implementation.registerCount - receiverWidth - parameterWidth
    }

internal fun Instruction.argumentRegister(parameterIndex: Int): Int {
    val reference = getReference<MethodReference>()
        ?: throw PatchException("Could not read argument registers of ${opcode.name}.")
    if (parameterIndex !in reference.parameterTypeNames.indices) {
        throw PatchException("Parameter $parameterIndex is not available in ${reference.smaliReference}.")
    }

    val instanceWidth = when (opcode) {
        Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE -> 0
        else -> 1
    }
    val offset = instanceWidth +
        reference.parameterTypeNames.take(parameterIndex).sumOf { it.registerWidth }

    return when (this) {
        is RegisterRangeInstruction -> startRegister + offset
        is FiveRegisterInstruction -> when (offset) {
            0 -> registerC
            1 -> registerD
            2 -> registerE
            3 -> registerF
            4 -> registerG
            else -> throw PatchException(
                "Argument $parameterIndex is out of range for ${reference.smaliReference}.",
            )
        }

        else -> throw PatchException("Could not read argument registers of ${opcode.name}.")
    }
}

internal fun Method.parameterRegister(parameterIndex: Int): Int {
    val implementation = implementation
        ?: throw PatchException("Could not inspect registers for $definingClass->$name.")
    if (parameterIndex !in parameterTypeNames.indices) {
        throw PatchException("Parameter $parameterIndex is not available in $definingClass->$name.")
    }

    val parameterWidth = parameterTypeNames.sumOf { it.registerWidth }
    return implementation.registerCount - parameterWidth +
        parameterTypeNames.take(parameterIndex).sumOf { it.registerWidth }
}
