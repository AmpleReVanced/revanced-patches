package app.revanced.patches.kakaotalk.integrity

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.integrity.fingerprints.moatCheckResultFingerprintOne
import app.revanced.patches.kakaotalk.integrity.fingerprints.moatCheckResultFingerprintTwo
import app.revanced.patches.kakaotalk.integrity.fingerprints.postprocessMoatCheckFailedFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference

@Suppress("unused")
val bypassMoatCheckPatch = bytecodePatch(
    name = "Bypass Moat check",
    description = "Bypass Moat check that prevents the app from running.",
) {
    compatibleWith("com.kakao.talk"("25.8.0"))

    execute {
        val patch: (Fingerprint) -> Unit = {
            val method = it.method
            val insns  = method.instructions

            insns
                .filterIsInstance<BuilderInstruction21c>()
                .filter { inst ->
                    inst.opcode    == Opcode.SGET_OBJECT &&
                            inst.reference == ImmutableFieldReference(
                        "Ljava/lang/Boolean;", "FALSE", "Ljava/lang/Boolean;"
                    )
                }
                .forEach { inst ->
                    val idx = insns.indexOf(inst)
                    method.replaceInstruction(
                        idx,
                        BuilderInstruction21c(
                            Opcode.SGET_OBJECT,
                            inst.registerA,
                            ImmutableFieldReference(
                                "Ljava/lang/Boolean;", "TRUE", "Ljava/lang/Boolean;"
                            )
                        )
                    )
                }

            val postprocessMoatCheckFailedMethod = postprocessMoatCheckFailedFingerprint.method

            val toRemove = mutableListOf<Instruction>()
            insns.forEachIndexed { i, inst ->
                if (inst is BuilderInstruction35c &&
                    inst.opcode == Opcode.INVOKE_VIRTUAL &&
                    (inst.getReference<MethodReference>()?.name == postprocessMoatCheckFailedMethod.name) &&
                    inst.getReference<MethodReference>()?.definingClass ==
                    "Lcom/kakaopay/shared/security/moat/PaySecurityWorker;"
                ) {
                    for (j in 0..3) {
                        insns.getOrNull(i + j)?.let { toRemove += it }
                    }
                }
            }
            toRemove
                .distinct()
                .sortedByDescending { insns.indexOf(it) }
                .forEach { inst ->
                    method.removeInstruction(insns.indexOf(inst))
                }
        }

        patch(moatCheckResultFingerprintOne)
        patch(moatCheckResultFingerprintTwo)
    }
}