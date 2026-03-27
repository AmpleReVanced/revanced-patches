package app.revanced.patches.dcinside.onestore

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n

@Suppress("unused")
val enableOnestoreFeaturePatch = bytecodePatch(
    name = "Enable OneStore feature",
    description = "Enables the OneStore feature in DC Inside app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        ApplicationConfigClassFingerprint.classDef.methods.forEach { method ->
            val isMatch = method.run {
                        parameterTypes.isEmpty() &&
                        returnType == "Z" &&
                        implementation?.instructions?.size == 2 &&
                        implementation?.instructions?.get(0)?.opcode == Opcode.CONST_4 &&
                        (implementation?.instructions?.get(0) as? BuilderInstruction11n)?.narrowLiteral == 0x0 &&
                        implementation?.instructions?.get(1)?.opcode == Opcode.RETURN
            }

            if (isMatch) {
                method.returnEarly(true)
            }
        }
    }
}
