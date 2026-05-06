package app.revanced.util

import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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