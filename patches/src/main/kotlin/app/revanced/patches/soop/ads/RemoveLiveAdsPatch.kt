package app.revanced.patches.soop.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.soop.ads.fingerprints.*
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val removeLiveAdsPatch = bytecodePatch(
    name = "Remove live ads",
    description = "Removes the live pre-roll ad by routing every ad decision into the app's own " +
        "no-video path so the stream starts immediately.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        val reducer = LiveAdDecisionFingerprint.method
        val instructions = reducer.instructions.toList()

        val decisionCastIndex = instructions.indexOfFirst { it.opcode == Opcode.CHECK_CAST }
        if (decisionCastIndex < 0) throw PatchException("Could not find the ad-decision cast.")

        val decisionBlockEnd = (instructions.first { it.opcode == Opcode.IF_EQZ } as BuilderInstruction21t)
            .target.location.index

        val (skip, emitMethod) = (decisionCastIndex + 1 until decisionBlockEnd)
            .asSequence()
            .mapNotNull { index ->
                val sget = instructions[index]
                val emit = instructions.getOrNull(index + 1)
                if (sget.opcode != Opcode.SGET_OBJECT || emit?.opcode != Opcode.INVOKE_VIRTUAL) return@mapNotNull null
                val emitReference = (emit as ReferenceInstruction).reference as? MethodReference ?: return@mapNotNull null
                if (emitReference.definingClass != reducer.definingClass ||
                    emitReference.returnType != "V" ||
                    emitReference.parameterTypes.size != 1
                ) return@mapNotNull null
                val skipReference = (sget as ReferenceInstruction).reference as? FieldReference ?: return@mapNotNull null
                skipReference to emitReference
            }
            .lastOrNull()
            ?: throw PatchException("Could not find the skip effect emitter.")

        reducer.addInstructions(
            decisionCastIndex + 1,
            """
                move-object/from16 v1, p0
                sget-object v0, ${skip.definingClass}->${skip.name}:${skip.type}
                invoke-virtual { v1, v0 }, ${emitMethod.definingClass}->${emitMethod.name}(${emitMethod.parameterTypes.first()})V
                return-void
            """,
        )
    }
}