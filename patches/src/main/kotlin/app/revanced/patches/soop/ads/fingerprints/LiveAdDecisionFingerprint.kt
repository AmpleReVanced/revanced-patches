package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val AD_VAST_RESPONSE = "Lcom/afreecatv/domain/advertisement/model/AdVastResponse;"

internal object LiveAdDecisionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    custom = { method, _ ->
        method.parameterTypes.size == 1 &&
            method.implementation?.instructions?.toList()?.let { instructions ->
                var referencesAdVastResponse = false
                var instanceOfCount = 0
                var effectEmitPairs = 0

                instructions.forEachIndexed { index, instruction ->
                    when (instruction.opcode) {
                        Opcode.INSTANCE_OF -> instanceOfCount++

                        Opcode.SGET_OBJECT -> {
                            val next = instructions.getOrNull(index + 1)
                            val reference = (next as? ReferenceInstruction)?.reference as? MethodReference
                            if (next?.opcode == Opcode.INVOKE_VIRTUAL &&
                                reference != null &&
                                reference.definingClass == method.definingClass &&
                                reference.returnType == "V" &&
                                reference.parameterTypes.size == 1
                            ) effectEmitPairs++
                        }

                        else -> {
                            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                            if (reference?.definingClass == AD_VAST_RESPONSE) referencesAdVastResponse = true
                        }
                    }
                }

                referencesAdVastResponse && instanceOfCount >= 3 && effectEmitPairs >= 2
            } == true
    },
)