package app.revanced.patches.kakaotalk.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.integrity.fingerprints.CheckApkChecksumsFingerprint
import app.revanced.patches.kakaotalk.integrity.fingerprints.MoatResultClassFingerprint
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/BypassMoatCheckPatch;"

@Suppress("unused")
val bypassMoatCheckPatch = bytecodePatch(
    name = "Bypass Moat check",
    description = "Add a setting to bypass the Moat integrity check that can prevent KakaoPay from running.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            SwitchPreference(
                key = "morphe_pref_bypass_moat_integrity_check",
                titleKey = "morphe_settings_patch_bypass_moat_check",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        CheckApkChecksumsFingerprint.method.apply {
            val lastSgetObjectType = instructions.last { it.opcode == Opcode.SGET_OBJECT }.getReference<FieldReference>()?.type

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->bypassMoatIntegrityCheck()Z
                    move-result v0
                    if-eqz v0, :morphe_original_moat_checksum
                    new-instance v0, Lkotlin/Pair;
                    sget-object v1, $lastSgetObjectType->VERIFIED:$lastSgetObjectType
                    const-string v2, ""
                    invoke-direct {v0, v1, v2}, Lkotlin/Pair;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                    return-object v0
                    :morphe_original_moat_checksum
                    nop
                """.trimIndent()
            )
        }

        val moatResultArrayType = "[${MoatResultClassFingerprint.classDef.type}"
        val nativeStatusAccessFlags =
            AccessFlags.STATIC.value or AccessFlags.FINAL.value or AccessFlags.NATIVE.value
        val nativeStatusMethods = buildList<Method> {
            classDefForEach { classDef ->
                classDef.methods.filterTo(this) { method ->
                    method.accessFlags and nativeStatusAccessFlags == nativeStatusAccessFlags &&
                            method.parameterTypes == listOf("I", "I") &&
                            method.returnType == moatResultArrayType
                }
            }
        }
        val nativeStatusMethod = nativeStatusMethods.singleOrNull()
            ?: throw PatchException("Expected one Moat native status method, found ${nativeStatusMethods.size}.")

        var patchedCallSites = 0
        buildMap {
            classDefForEach { classDef ->
                val methodCallSites = classDef.methods.mapNotNull { method ->
                    val instructions = method.implementation?.instructions?.toList()
                        ?: return@mapNotNull null

                    val invokeIndices = instructions.mapIndexedNotNull { index, instruction ->
                        val reference = instruction.getReference<MethodReference>()
                        index.takeIf {
                            (instruction.opcode == Opcode.INVOKE_STATIC || instruction.opcode == Opcode.INVOKE_STATIC_RANGE) &&
                                    reference?.definingClass == nativeStatusMethod.definingClass &&
                                    reference.name == nativeStatusMethod.name &&
                                    reference.parameterTypes == nativeStatusMethod.parameterTypes &&
                                    reference.returnType == nativeStatusMethod.returnType &&
                                    instructions.getOrNull(index + 1)?.opcode == Opcode.MOVE_RESULT_OBJECT
                        }
                    }

                    invokeIndices.takeIf { it.isNotEmpty() }?.let { method to it }
                }

                if (methodCallSites.isNotEmpty()) {
                    put(classDef, methodCallSites)
                }
            }
        }.forEach { (classDef, methodCallSites) ->
            val mutableClass = mutableClassDefBy(classDef)
            methodCallSites.forEach { (method, invokeIndices) ->
                val mutableMethod = mutableClass.findMutableMethodOf(method)

                invokeIndices.asReversed().forEach { invokeIndex ->
                    val invokeInstruction = mutableMethod.getInstruction(invokeIndex)
                    val (parameterRegisters, originalInvoke) = when (invokeInstruction) {
                        is FiveRegisterInstruction -> {
                            val registers = listOf(
                                invokeInstruction.registerC,
                                invokeInstruction.registerD,
                                invokeInstruction.registerE,
                                invokeInstruction.registerF,
                                invokeInstruction.registerG,
                            ).take(invokeInstruction.registerCount)

                            registers to "invoke-static {${registers.joinToString(", ") { "v$it" }}}, $nativeStatusMethod"
                        }

                        is RegisterRangeInstruction -> {
                            val startRegister = invokeInstruction.startRegister
                            val registers = (startRegister until startRegister + invokeInstruction.registerCount).toList()
                            val endRegister = registers.last()

                            registers to "invoke-static/range {v$startRegister .. v$endRegister}, $nativeStatusMethod"
                        }

                        else -> throw PatchException(
                            "Unsupported Moat native status invoke instruction: ${invokeInstruction.opcode}"
                        )
                    }
                    val resultRegister = (mutableMethod.getInstruction(invokeIndex + 1) as OneRegisterInstruction).registerA
                    val tempRegister = mutableMethod.getFreeRegisterProvider(
                        invokeIndex,
                        1,
                        *(parameterRegisters + resultRegister).toIntArray(),
                    ).getFreeRegister()
                    val originalLabel = "morphe_original_moat_native_status_$invokeIndex"
                    val afterLabel = "morphe_after_moat_native_status_$invokeIndex"

                    mutableMethod.removeInstructions(invokeIndex, 2)
                    mutableMethod.addInstructionsWithLabels(
                        invokeIndex,
                        """
                            invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->bypassMoatIntegrityCheck()Z
                            move-result v$tempRegister
                            if-eqz v$tempRegister, :$originalLabel
                            const/4 v$tempRegister, 0x0
                            new-array v$resultRegister, v$tempRegister, ${nativeStatusMethod.returnType}
                            goto :$afterLabel
                            :$originalLabel
                            $originalInvoke
                            move-result-object v$resultRegister
                            :$afterLabel
                            nop
                        """.trimIndent()
                    )
                    patchedCallSites++
                }
            }
        }

        if (patchedCallSites == 0) {
            throw PatchException("Could not find any Moat native status call sites.")
        }
    }
}
