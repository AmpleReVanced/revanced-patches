package app.revanced.patches.chzzk.common.utils

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal fun MutableClass.forceBooleanGetterTrue(fieldName: String) = forceBooleanGetter(fieldName, true)

internal fun MutableClass.forceBooleanGetterFalse(fieldName: String) = forceBooleanGetter(fieldName, false)

private fun MutableClass.forceBooleanGetter(fieldName: String, value: Boolean) {
    val getters = fieldGetters(fieldName, Opcode.IGET_BOOLEAN, Opcode.RETURN)
    if (getters.isEmpty()) {
        throw PatchException("Could not find boolean getter for $type->$fieldName.")
    }

    getters.forEach { it.returnEarly(value) }
}

internal fun MutableClass.forceObjectGetterNull(fieldName: String) {
    val getters = fieldGetters(fieldName, Opcode.IGET_OBJECT, Opcode.RETURN_OBJECT)
    if (getters.isEmpty()) {
        throw PatchException("Could not find object getter for $type->$fieldName.")
    }

    getters.forEach { it.returnEarly(null) }
}

private fun MutableClass.fieldGetters(
    fieldName: String,
    getOpcode: Opcode,
    returnOpcode: Opcode,
): List<MutableMethod> = methods.filter { it.isFieldGetter(type, fieldName, getOpcode, returnOpcode) }

private fun Method.isFieldGetter(
    owner: String,
    fieldName: String,
    getOpcode: Opcode,
    returnOpcode: Opcode,
): Boolean {
    if (parameterTypes.isNotEmpty()) return false

    val instructions = implementation?.instructions?.toList() ?: return false
    if (instructions.size != 2) return false

    val (fieldGet, returnValue) = instructions
    if (fieldGet.opcode != getOpcode || returnValue.opcode != returnOpcode) return false

    val valueRegister = (fieldGet as? TwoRegisterInstruction)?.registerA ?: return false
    val returnRegister = (returnValue as? OneRegisterInstruction)?.registerA ?: return false
    if (valueRegister != returnRegister) return false

    val field = fieldGet.getReference<FieldReference>() ?: return false
    return field.definingClass == owner && field.name == fieldName && returnType == field.type
}