package app.revanced.patches.kakaotalk.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.integrity.fingerprints.CheckApkChecksumsFingerprint
import app.revanced.patches.kakaotalk.integrity.fingerprints.MoatResultClassFingerprint
import app.revanced.patches.kakaotalk.integrity.fingerprints.MoatScanDispatcherFingerprint
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.parameterTypeNames
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
private const val BYPASS_MOAT =
    "Lapp/revanced/extension/kakaotalk/settings/Settings;->bypassMoatIntegrityCheck()Z"
private val NATIVE_FLAGS =
    AccessFlags.STATIC.value or AccessFlags.FINAL.value or AccessFlags.NATIVE.value

@Suppress("unused")
val bypassMoatCheckPatch = bytecodePatch(
    name = "Bypass Moat check",
    description = "Add a setting to bypass the KakaoPay Moat integrity check. It stops the native " +
            "scan from running, so the tamper/root/hook verdict is never computed or reported and " +
            "KakaoPay is not force-closed. Payments on a modified build are still risky.",
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

        MoatScanDispatcherFingerprint.method.apply {
            val callbackType = parameterTypeNames[1]
            val callbackMethodName = classDefBy(callbackType).methods.first { method ->
                method.returnType == "V" &&
                        method.parameterTypeNames == listOf("Ljava/util/List;", "Ljava/lang/String;", "Ljava/lang/String;")
            }.name
            val free = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $BYPASS_MOAT
                    move-result v$free
                    if-eqz v$free, :morphe_moat_scan
                    invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                    move-result-object v$free
                    invoke-interface {p2, v$free, p3, p4}, $callbackType->$callbackMethodName(Ljava/util/List;Ljava/lang/String;Ljava/lang/String;)V
                    const/4 v$free, 0x0
                    invoke-static {v$free}, Ljava/util/concurrent/CompletableFuture;->completedFuture(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;
                    move-result-object v$free
                    return-object v$free
                    :morphe_moat_scan
                    nop
                """.trimIndent(),
            )
        }

        CheckApkChecksumsFingerprint.method.apply {
            val verifiedType = instructions.last { it.opcode == Opcode.SGET_OBJECT }
                .getReference<FieldReference>()?.type

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $BYPASS_MOAT
                    move-result v0
                    if-eqz v0, :morphe_original_moat_checksum
                    new-instance v0, Lkotlin/Pair;
                    sget-object v1, $verifiedType->VERIFIED:$verifiedType
                    const-string v2, ""
                    invoke-direct {v0, v1, v2}, Lkotlin/Pair;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                    return-object v0
                    :morphe_original_moat_checksum
                    nop
                """.trimIndent(),
            )
        }

        // Rewrite every invoke-static call site of a native Moat method, latest index first so the
        // earlier ones stay valid after each edit.
        fun forEachCallSite(target: Method, gate: MutableMethod.(Int) -> Unit) {
            classDefForEach { classDef ->
                val callSites = classDef.methods.mapNotNull { method ->
                    val methodInstructions = method.implementation?.instructions?.toList() ?: return@mapNotNull null
                    methodInstructions.indices.filter { index ->
                        val instruction = methodInstructions[index]
                        val reference = instruction.getReference<MethodReference>()
                        (instruction.opcode == Opcode.INVOKE_STATIC || instruction.opcode == Opcode.INVOKE_STATIC_RANGE) &&
                                reference?.definingClass == target.definingClass &&
                                reference.name == target.name &&
                                reference.parameterTypes == target.parameterTypes &&
                                reference.returnType == target.returnType
                    }.takeIf { it.isNotEmpty() }?.let { method to it }
                }

                callSites.forEach { (method, indices) ->
                    val mutableMethod = mutableClassDefBy(classDef).findMutableMethodOf(method)
                    indices.asReversed().forEach { mutableMethod.gate(it) }
                }
            }
        }

        val moatResultArrayType = "[${MoatResultClassFingerprint.classDef.type}"
        val nativeStatusMethods = buildList {
            classDefForEach { classDef ->
                classDef.methods.filterTo(this) { method ->
                    method.accessFlags and NATIVE_FLAGS == NATIVE_FLAGS &&
                            method.parameterTypes == listOf("I", "I") &&
                            method.returnType == moatResultArrayType
                }
            }
        }
        val nativeStatusMethod = nativeStatusMethods.singleOrNull()
            ?: throw PatchException("Expected one Moat native status method, found ${nativeStatusMethods.size}.")

        var patchedStatusReads = 0
        forEachCallSite(nativeStatusMethod) { index ->
            val resultInstruction = getInstruction(index + 1)
            if (resultInstruction.opcode != Opcode.MOVE_RESULT_OBJECT) return@forEachCallSite
            val resultRegister = (resultInstruction as OneRegisterInstruction).registerA

            val invoke = getInstruction(index)
            val (registers, originalInvoke) = when (invoke) {
                is FiveRegisterInstruction -> {
                    val registers = listOf(invoke.registerC, invoke.registerD, invoke.registerE, invoke.registerF, invoke.registerG)
                        .take(invoke.registerCount)
                    registers to "invoke-static {${registers.joinToString(", ") { "v$it" }}}, $nativeStatusMethod"
                }

                is RegisterRangeInstruction -> {
                    val registers = (invoke.startRegister until invoke.startRegister + invoke.registerCount).toList()
                    registers to "invoke-static/range {v${invoke.startRegister} .. v${registers.last()}}, $nativeStatusMethod"
                }

                else -> throw PatchException("Unsupported Moat invoke instruction: ${invoke.opcode}")
            }
            val temp = getFreeRegisterProvider(index, 1, *(registers + resultRegister).toIntArray()).getFreeRegister4Bit()

            removeInstructions(index, 2)
            addInstructionsWithLabels(
                index,
                """
                    invoke-static {}, $BYPASS_MOAT
                    move-result v$temp
                    if-eqz v$temp, :morphe_original_status_$index
                    const/4 v$temp, 0x0
                    new-array v$temp, v$temp, $moatResultArrayType
                    move-object/from16 v$resultRegister, v$temp
                    goto :morphe_after_status_$index
                    :morphe_original_status_$index
                    $originalInvoke
                    move-result-object v$resultRegister
                    :morphe_after_status_$index
                    nop
                """.trimIndent(),
            )
            patchedStatusReads++
        }

        if (patchedStatusReads == 0) {
            throw PatchException("Could not find any Moat native status call sites.")
        }
    }
}
