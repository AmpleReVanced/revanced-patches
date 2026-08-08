package app.revanced.patches.soop.common.utils

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.iface.ClassDef

internal fun MutableMethod.returnUnitEarly(unitClass: ClassDef) {
    val instance = unitClass.staticFields.singleOrNull { it.type == unitClass.type }
        ?: throw PatchException("Could not find the Kotlin Unit instance field.")

    addInstructions(
        0,
        """
            sget-object v0, ${instance.definingClass}->${instance.name}:${instance.type}
            return-object v0
        """,
    )
}