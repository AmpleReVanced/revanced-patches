package app.revanced.patches.kakaotalk.misc.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.numberOfParameterRegisters
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.CheckApkChecksumsFingerprint
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.MoatLibraryLoaderFingerprint
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.MoatResultClassFingerprint
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.MoatScanDispatcherFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.parameterTypeNames
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.VariableRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/BypassMoatCheckPatch;"
private const val BYPASS_MOAT =
    "Lapp/revanced/extension/kakaotalk/settings/Settings;->bypassMoatIntegrityCheck()Z"
private const val MOAT_STATUS_GATE_METHOD = "revanced_moatStatusGate"
private const val MOAT_INITIALIZE_GATE_METHOD = "revanced_moatInitializeGate"
private const val MOAT_UPDATE_GATE_METHOD = "revanced_moatUpdateGate"
private const val MOAT_PATTERN_GATE_METHOD = "revanced_moatPatternGate"
private const val MOAT_PACKAGES_GATE_METHOD = "revanced_moatPackagesGate"
private const val MOAT_SUPPORT_GATE_METHOD = "revanced_moatSupportGate"
private const val MOAT_LOG_GATE_METHOD = "revanced_moatLogGate"
private const val CONTEXT_TYPE = "Landroid/content/Context;"
private const val STRING_TYPE = "Ljava/lang/String;"
private const val MAP_TYPE = "Ljava/util/Map;"
private const val OS_SUPPORT_VALUE = """{\"is_supported\":true}"""
private val NATIVE_FLAGS =
    AccessFlags.STATIC.value or AccessFlags.FINAL.value or AccessFlags.NATIVE.value
private val NATIVE_READER_PARAMETERS =
    listOf(listOf("I"), listOf("I", "I"), listOf("I", "I", "I"))

private class MoatNativeGate(
    val target: Method,
    val name: String,
    val localRegisters: Int,
    val bypassInstructions: String,
    val purpose: String,
) {
    var callSites = 0
}

@Suppress("unused")
val bypassMoatCheckPatch = bytecodePatch(
    name = "Bypass Moat check",
    description = "Adds a setting to prevent KakaoPay Moat initialization, policy and pattern " +
            "updates, scans, detector logging, and force-off reports while returning benign " +
            "integrity results. Payments on a modified build are still risky.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.ADVANCED.addPreferences(
            SwitchPreference(
                key = "morphe_pref_bypass_moat_integrity_check",
                titleKey = "morphe_settings_patch_bypass_moat_check",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        MoatLibraryLoaderFingerprint.method.apply {
            val free = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $BYPASS_MOAT
                    move-result v$free
                    if-eqz v$free, :morphe_moat_library
                    return-void
                    :morphe_moat_library
                    nop
                """.trimIndent(),
            )
        }

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

        fun MutableMethod.replaceStaticInvoke(index: Int, descriptor: String) {
            val invoke = getInstruction(index)
            val newInvoke = when (invoke) {
                is FiveRegisterInstruction -> {
                    val registers = listOf(invoke.registerC, invoke.registerD, invoke.registerE, invoke.registerF, invoke.registerG)
                        .take((invoke as VariableRegisterInstruction).registerCount)
                    "invoke-static {${registers.joinToString(", ") { "v$it" }}}, $descriptor"
                }

                is RegisterRangeInstruction -> {
                    val last = invoke.startRegister + (invoke as VariableRegisterInstruction).registerCount - 1
                    "invoke-static/range {v${invoke.startRegister} .. v$last}, $descriptor"
                }

                else -> throw PatchException("Unsupported Moat invoke instruction: ${invoke.opcode}")
            }
            replaceInstruction(index, newInvoke)
        }

        fun addNativeGate(target: Method, gateMethod: String, localRegisters: Int, bypassInstructions: String) {
            val parameters = target.parameterTypes
            val originalInvoke = "${target.definingClass}->${target.name}" +
                    "(${parameters.joinToString("")})${target.returnType}"
            val originalRegisters = (0 until target.numberOfParameterRegisters)
                .joinToString(", ") { "p$it" }
            val originalReturn = when {
                target.returnType == "V" -> "return-void"
                target.returnType == "J" || target.returnType == "D" ->
                    "move-result-wide v0\nreturn-wide v0"
                target.returnType.startsWith("L") || target.returnType.startsWith("[") ->
                    "move-result-object v0\nreturn-object v0"
                else -> "move-result v0\nreturn v0"
            }
            mutableClassDefBy(target.definingClass).methods.add(
                ImmutableMethod(
                    target.definingClass,
                    gateMethod,
                    parameters.map { ImmutableMethodParameter(it.toString(), null, null) },
                    target.returnType,
                    AccessFlags.PUBLIC.value or AccessFlags.STATIC.value or AccessFlags.FINAL.value,
                    null,
                    null,
                    MutableMethodImplementation(target.numberOfParameterRegisters + localRegisters),
                ).toMutable().apply {
                    addInstructionsWithLabels(
                        0,
                        """
                            invoke-static {}, $BYPASS_MOAT
                            move-result v0
                            if-eqz v0, :morphe_original
                            $bypassInstructions
                            :morphe_original
                            invoke-static {$originalRegisters}, $originalInvoke
                            $originalReturn
                        """.trimIndent(),
                    )
                },
            )
        }

        val nativeGates = mutableListOf<MoatNativeGate>()

        fun gateNativeCalls(
            target: Method,
            gateMethod: String,
            localRegisters: Int,
            bypassInstructions: String,
            purpose: String,
        ) {
            nativeGates.add(MoatNativeGate(target, gateMethod, localRegisters, bypassInstructions, purpose))
        }

        val moatResultArrayType = "[${MoatResultClassFingerprint.classDef.type}"
        val moatNativeClass = MoatLibraryLoaderFingerprint.classDef
        if (moatNativeClass.methods.none { method ->
                method.accessFlags and NATIVE_FLAGS == NATIVE_FLAGS &&
                        method.returnType == moatResultArrayType
            }) {
            throw PatchException("Moat library loader has no native status reader.")
        }

        fun ClassDef.nativeMethods(
            returnType: String,
            parameters: List<String>,
        ) = methods.filter { method ->
            method.accessFlags and NATIVE_FLAGS == NATIVE_FLAGS &&
                    method.parameterTypes == parameters &&
                    method.returnType == returnType
        }

        fun ClassDef.singleNativeMethod(
            returnType: String,
            parameters: List<String>,
            purpose: String,
        ): Method {
            val matches = nativeMethods(returnType, parameters)
            return matches.singleOrNull()
                ?: throw PatchException("Expected one Moat $purpose method, found ${matches.size}.")
        }

        NATIVE_READER_PARAMETERS.forEach { parameters ->
            val nativeStatusMethod = moatNativeClass.singleNativeMethod(
                moatResultArrayType,
                parameters,
                "status reader with ${parameters.size} parameter(s)",
            )
            gateNativeCalls(
                nativeStatusMethod,
                MOAT_STATUS_GATE_METHOD,
                1,
                """
                    const/4 v0, 0x0
                    new-array v0, v0, $moatResultArrayType
                    return-object v0
                """.trimIndent(),
                "status reader with ${parameters.size} parameter(s)",
            )
        }

        val nativeInitializeMethod = moatNativeClass.singleNativeMethod(
            moatResultArrayType,
            listOf(CONTEXT_TYPE, "J"),
            "SDK initialization",
        )
        gateNativeCalls(
            nativeInitializeMethod,
            MOAT_INITIALIZE_GATE_METHOD,
            1,
            """
                const/4 v0, 0x0
                new-array v0, v0, $moatResultArrayType
                return-object v0
            """.trimIndent(),
            "SDK initialization",
        )

        val nativeUpdateMethods = moatNativeClass.nativeMethods(
            "V",
            listOf(STRING_TYPE, MAP_TYPE),
        )
        if (nativeUpdateMethods.size != 3) {
            throw PatchException(
                "Expected three Moat policy and pattern update methods, found ${nativeUpdateMethods.size}.",
            )
        }
        nativeUpdateMethods.forEachIndexed { index, nativeUpdateMethod ->
            gateNativeCalls(
                nativeUpdateMethod,
                "$MOAT_UPDATE_GATE_METHOD$index",
                1,
                "return-void",
                "policy or pattern update",
            )
        }

        val nativePatternMethod = moatNativeClass.singleNativeMethod(
            "Z",
            listOf(STRING_TYPE, MAP_TYPE),
            "malware pattern update",
        )
        gateNativeCalls(
            nativePatternMethod,
            MOAT_PATTERN_GATE_METHOD,
            1,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
            "malware pattern update",
        )

        val nativePackagesMethod = moatNativeClass.singleNativeMethod(
            "Ljava/util/List;",
            listOf(CONTEXT_TYPE),
            "unknown source packages reader",
        )
        gateNativeCalls(
            nativePackagesMethod,
            MOAT_PACKAGES_GATE_METHOD,
            1,
            """
                invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                move-result-object v0
                return-object v0
            """.trimIndent(),
            "unknown source packages reader",
        )

        val nativeSupportMethod = moatNativeClass.singleNativeMethod(
            "Lkotlin/Pair;",
            listOf(CONTEXT_TYPE),
            "OS version support reader",
        )
        gateNativeCalls(
            nativeSupportMethod,
            MOAT_SUPPORT_GATE_METHOD,
            3,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                const-string v1, "$OS_SUPPORT_VALUE"
                new-instance v2, Lkotlin/Pair;
                invoke-direct {v2, v0, v1}, Lkotlin/Pair;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                return-object v2
            """.trimIndent(),
            "OS version support reader",
        )

        val nativeLogMethod = moatNativeClass.singleNativeMethod(
            "I",
            listOf("Ljava/lang/String;", "Ljava/lang/Object;", "Ljava/lang/String;"),
            "detector logger",
        )
        gateNativeCalls(
            nativeLogMethod,
            MOAT_LOG_GATE_METHOD,
            1,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
            "detector logging",
        )

        val gatesByReference = nativeGates.associateBy { it.target.toString() }
        if (gatesByReference.size != nativeGates.size) {
            throw PatchException("Moat native gate targets are not unique.")
        }
        val callSites = mutableListOf<Pair<Method, List<Pair<Int, MoatNativeGate>>>>()
        classDefForEach { classDef ->
            for (method in classDef.methods) {
                if (method.name.startsWith("revanced_moat")) continue
                val implementation = method.implementation ?: continue
                val methodCallSites = mutableListOf<Pair<Int, MoatNativeGate>>()
                for ((index, instruction) in implementation.instructions.withIndex()) {
                    if (instruction.opcode != Opcode.INVOKE_STATIC &&
                        instruction.opcode != Opcode.INVOKE_STATIC_RANGE
                    ) continue
                    val reference = instruction.getReference<MethodReference>() ?: continue
                    if (reference.definingClass != moatNativeClass.type) continue
                    val gate = gatesByReference[reference.toString()] ?: continue
                    methodCallSites.add(index to gate)
                    gate.callSites++
                }
                if (methodCallSites.isNotEmpty()) callSites.add(method to methodCallSites)
            }
        }
        nativeGates.forEach { gate ->
            if (gate.callSites == 0) {
                throw PatchException("Could not find any Moat ${gate.purpose} call sites.")
            }
            addNativeGate(gate.target, gate.name, gate.localRegisters, gate.bypassInstructions)
        }
        val gateDescriptors = nativeGates.associateWith { gate ->
            "${gate.target.definingClass}->${gate.name}" +
                    "(${gate.target.parameterTypes.joinToString("")})${gate.target.returnType}"
        }
        callSites.forEach { (method, matches) ->
            val mutableMethod = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            matches.asReversed().forEach { (index, gate) ->
                mutableMethod.replaceStaticInvoke(index, gateDescriptors.getValue(gate))
            }
        }
    }
}
