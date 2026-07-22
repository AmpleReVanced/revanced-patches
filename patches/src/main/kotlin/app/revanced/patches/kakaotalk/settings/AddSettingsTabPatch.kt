package app.revanced.patches.kakaotalk.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.util.cloneMutable
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.argumentRegister
import app.revanced.util.parameterTypeNames
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val ADD_MORPHE_SETTINGS_ITEM_METHOD = "revanced_addMorpheSettingsItem"
private const val DEFAULT_CONSTRUCTOR_MARKER = "Lkotlin/jvm/internal/DefaultConstructorMarker;"

@Suppress("unused")
val addSettingsTabPatch = bytecodePatch(
    name = "Add settings tab",
    description = "Adds a settings tab to the app.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(
        addExtensionPatch,
        addResourcesPatch,
        addSettingsResourcesPatch,
        sharedExtensionPatch,
        registerSettingsActivityPatch
    )

    execute {
        syncThemeNightModePreference()

        val mainSettingItemTypeClass = MainSettingItemTypeFingerprint.classDef

        mainSettingItemTypeClass.fields.add(
            ImmutableField(
                mainSettingItemTypeClass.type,
                "MORPHE",
                mainSettingItemTypeClass.type,
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value or AccessFlags.FINAL.value or AccessFlags.ENUM.value,
                null,
                null,
                null
            ).toMutable()
        )

        val valuesMethod = mainSettingItemTypeClass.methods.find { it.name == "\$values" }
            ?: throw PatchException("Could not find \$values method")
        val valuesArrayIndex = valuesMethod.instructions.indexOfFirst {
            it.opcode == Opcode.FILLED_NEW_ARRAY_RANGE
        }.takeIf { it >= 0 }
            ?: throw PatchException("Could not find enum values array construction.")
        val valuesArrayInstruction = valuesMethod.getInstruction(valuesArrayIndex)
        val valuesArrayRange = valuesArrayInstruction as? RegisterRangeInstruction
            ?: throw PatchException("Enum values array is not a range instruction.")
        val valuesArrayType = valuesArrayInstruction.getReference<TypeReference>()?.type
            ?: throw PatchException("Could not resolve enum values array type.")
        val valuesRegisterCount = valuesMethod.implementation?.registerCount
            ?: throw PatchException("Could not inspect \$values register count.")
        val morpheOrdinal = valuesArrayRange.registerCount
        val morpheRegister = valuesArrayRange.startRegister + valuesArrayRange.registerCount

        mainSettingItemTypeClass.methods.remove(valuesMethod)

        mainSettingItemTypeClass.methods.add(
            ImmutableMethod(
                mainSettingItemTypeClass.type,
                "\$values",
                listOf(),
                "[${mainSettingItemTypeClass.type}",
                valuesMethod.accessFlags,
                null,
                null,
                MutableMethodImplementation(valuesRegisterCount + 1),
            ).toMutable().apply {
                addInstructions(valuesMethod.instructions)

                if (morpheRegister >= valuesRegisterCount + 1) {
                    throw PatchException("Could not reserve register for MORPHE enum value.")
                }

                replaceInstruction(
                    valuesArrayIndex,
                    "filled-new-array/range {v${valuesArrayRange.startRegister} .. v$morpheRegister}, $valuesArrayType"
                )
                addInstructions(valuesArrayIndex, """
                    sget-object v$morpheRegister, ${mainSettingItemTypeClass.type}->MORPHE:${mainSettingItemTypeClass.type}
                """)
            }
        )

        val clinitMethod = mainSettingItemTypeClass.methods.find { it.name == "<clinit>" }
            ?: throw PatchException("Could not find <clinit> method")

        val morpheClinitRegisters = 6
        val clinitRegisterCount = clinitMethod.implementation?.registerCount
            ?: throw PatchException("Could not inspect <clinit> register count.")

        val mutableClinitMethod = if (clinitRegisterCount >= morpheClinitRegisters) {
            clinitMethod
        } else {
            clinitMethod.cloneMutable(additionalRegisters = morpheClinitRegisters - clinitRegisterCount).also {
                mainSettingItemTypeClass.methods.remove(clinitMethod)
                mainSettingItemTypeClass.methods.add(it)
            }
        }

        val insertIndex = mutableClinitMethod.instructions.indexOfLast {
            it.opcode == Opcode.SPUT_OBJECT &&
                    it.getReference<FieldReference>()?.name == "LANGUAGE"
        }.takeIf { it >= 0 }
            ?: throw PatchException("Could not find LANGUAGE enum initialization.")

        val clinitRegisters = mutableClinitMethod.getFreeRegisterProvider(insertIndex + 1, morpheClinitRegisters)
        val instanceRegister = clinitRegisters.getFreeRegister4Bit()
        val nameRegister = clinitRegisters.getFreeRegister4Bit()
        val ordinalRegister = clinitRegisters.getFreeRegister4Bit()
        val stringResIdRegister = clinitRegisters.getFreeRegister4Bit()
        val drawableResIdRegister = clinitRegisters.getFreeRegister4Bit()
        val resourceTypeRegister = clinitRegisters.getFreeRegister4Bit()

        mutableClinitMethod.addInstructions(insertIndex + 1, """
            new-instance v$instanceRegister, ${mainSettingItemTypeClass.type}
            const-string v$nameRegister, "MORPHE"
            const/16 v$ordinalRegister, 0x${morpheOrdinal.toString(16)}
            const-string v$stringResIdRegister, "morphe_label_for_ample_settings"
            const-string v$resourceTypeRegister, "string"
            invoke-static {v$resourceTypeRegister, v$stringResIdRegister}, Lapp/revanced/extension/kakaotalk/helper/ResourceHelper;->getResourceId(Ljava/lang/String;Ljava/lang/String;)I
            move-result v$stringResIdRegister
            const-string v$drawableResIdRegister, "morphe_settings_icon_dynamic"
            const-string v$resourceTypeRegister, "drawable"
            invoke-static {v$resourceTypeRegister, v$drawableResIdRegister}, Lapp/revanced/extension/kakaotalk/helper/ResourceHelper;->getResourceId(Ljava/lang/String;Ljava/lang/String;)I
            move-result v$drawableResIdRegister
            invoke-direct {v$instanceRegister, v$nameRegister, v$ordinalRegister, v$stringResIdRegister, v$drawableResIdRegister}, ${mainSettingItemTypeClass.type}-><init>(Ljava/lang/String;III)V
            sput-object v$instanceRegister, ${mainSettingItemTypeClass.type}->MORPHE:${mainSettingItemTypeClass.type}
        """)

        val setupSettingsItemMethod = SetupSettingsItemFingerprint.method

        val laboratoryTypeIndex = setupSettingsItemMethod.instructions.indexOfFirst { instruction ->
            if (instruction.opcode != Opcode.SGET_OBJECT) {
                return@indexOfFirst false
            }

            val reference = instruction.getReference<FieldReference>() ?: return@indexOfFirst false
            reference.definingClass == mainSettingItemTypeClass.type && reference.name == "LABORATORY"
        }.takeIf { it >= 0 }
            ?: throw PatchException("Could not find the Laboratory settings item.")
        val laboratoryRegister =
            (setupSettingsItemMethod.getInstruction(laboratoryTypeIndex) as OneRegisterInstruction).registerA

        val settingsModelConstructorIndex =
            (laboratoryTypeIndex + 1 until setupSettingsItemMethod.instructions.size).firstOrNull { index ->
                val instruction = setupSettingsItemMethod.instructions[index]
                if (instruction.opcode != Opcode.INVOKE_DIRECT &&
                    instruction.opcode != Opcode.INVOKE_DIRECT_RANGE
                ) {
                    return@firstOrNull false
                }

                val reference = instruction.getReference<MethodReference>() ?: return@firstOrNull false
                reference.name == "<init>" &&
                        reference.parameterTypeNames.firstOrNull() == mainSettingItemTypeClass.type &&
                        instruction.argumentRegister(0) == laboratoryRegister
            } ?: throw PatchException("Could not find the Laboratory settings model constructor.")
        val settingsModelConstructorInstruction =
            setupSettingsItemMethod.getInstruction(settingsModelConstructorIndex) as? RegisterRangeInstruction
                ?: throw PatchException("Settings model constructor is not a range instruction.")
        val finishSetupSettingsModel =
            setupSettingsItemMethod.instructions[settingsModelConstructorIndex].getReference<MethodReference>()
                ?: throw PatchException("Could not resolve the settings model constructor.")
        if (settingsModelConstructorInstruction.registerCount != 6) {
            throw PatchException("Unexpected settings model constructor register count.")
        }

        val initialSettingsItemType = finishSetupSettingsModel.parameterTypeNames.singleOrNull { type ->
            type.startsWith("L") &&
                    type != mainSettingItemTypeClass.type &&
                    type != DEFAULT_CONSTRUCTOR_MARKER
        } ?: throw PatchException("Could not resolve the settings item type.")

        val initialSettingsItemIndex = (laboratoryTypeIndex until settingsModelConstructorIndex).firstOrNull { index ->
            val instruction = setupSettingsItemMethod.instructions[index]
            if (instruction.opcode != Opcode.INVOKE_DIRECT) {
                return@firstOrNull false
            }

            val reference = instruction.getReference<MethodReference>() ?: return@firstOrNull false
            reference.definingClass == initialSettingsItemType && reference.name == "<init>"
        } ?: throw PatchException("Could not find the Laboratory settings item constructor.")
        val initialSettingsItemInstruction =
            setupSettingsItemMethod.getInstruction(initialSettingsItemIndex) as? FiveRegisterInstruction
                ?: throw PatchException("Settings item constructor is not a normal invoke instruction.")
        val initialSettingsItemReference =
            setupSettingsItemMethod.instructions[initialSettingsItemIndex].getReference<MethodReference>()
                ?: throw PatchException("Could not resolve the settings item constructor.")
        if (initialSettingsItemInstruction.registerCount != 5) {
            throw PatchException("Unexpected settings item constructor register count.")
        }

        val trackingActionIndex = (laboratoryTypeIndex until initialSettingsItemIndex).firstOrNull { index ->
            val instruction = setupSettingsItemMethod.instructions[index]
            if (instruction.opcode != Opcode.INVOKE_VIRTUAL) {
                return@firstOrNull false
            }

            val reference = instruction.getReference<MethodReference>() ?: return@firstOrNull false
            reference.name == "action" &&
                    reference.returnType.startsWith("L") &&
                    reference.parameterTypeNames == listOf("I")
        } ?: throw PatchException("Could not find the tracking action call.")
        val trackingActionInstruction =
            setupSettingsItemMethod.getInstruction(trackingActionIndex) as? FiveRegisterInstruction
                ?: throw PatchException("Tracking action call is not a normal invoke instruction.")
        val trackingActionReference =
            setupSettingsItemMethod.instructions[trackingActionIndex].getReference<MethodReference>()
                ?: throw PatchException("Could not resolve the tracking action reference.")
        val trackingActionReceiverRegister = trackingActionInstruction.registerC
        val trackingActionRegister = trackingActionInstruction.argumentRegister(0)
        val trackingActionArgument = (trackingActionIndex - 1 downTo laboratoryTypeIndex).firstNotNullOfOrNull { index ->
            val instruction = setupSettingsItemMethod.instructions[index]
            if (instruction !is NarrowLiteralInstruction ||
                (instruction as OneRegisterInstruction).registerA != trackingActionRegister
            ) {
                return@firstNotNullOfOrNull null
            }

            instruction.narrowLiteral
        } ?: throw PatchException("Could not resolve the tracking action argument.")

        val settingsItemHostParameterIndex = initialSettingsItemReference.parameterTypeNames
            .withIndex()
            .filter { (_, type) ->
                type.startsWith("L") &&
                        type != "Ljava/lang/String;" &&
                        type != "Landroid/content/Intent;" &&
                        type != trackingActionReference.returnType &&
                        type != DEFAULT_CONSTRUCTOR_MARKER
            }
            .singleOrNull()?.index
            ?: throw PatchException("Could not resolve the settings item host parameter.")
        val settingsItemHostRegister =
            initialSettingsItemInstruction.argumentRegister(settingsItemHostParameterIndex)
        val settingsItemHostField = (initialSettingsItemIndex - 1 downTo laboratoryTypeIndex).firstNotNullOfOrNull { index ->
            val instruction = setupSettingsItemMethod.instructions[index]
            if (instruction.opcode != Opcode.SGET_OBJECT ||
                (instruction as OneRegisterInstruction).registerA != settingsItemHostRegister
            ) {
                return@firstNotNullOfOrNull null
            }

            instruction.getReference<FieldReference>()
        } ?: throw PatchException("Could not resolve the settings item host field.")

        val contextRegister = (laboratoryTypeIndex until initialSettingsItemIndex).firstNotNullOfOrNull { index ->
            val instruction = setupSettingsItemMethod.instructions[index]
            if (instruction.opcode != Opcode.INVOKE_DIRECT &&
                instruction.opcode != Opcode.INVOKE_DIRECT_RANGE
            ) {
                return@firstNotNullOfOrNull null
            }

            val reference = instruction.getReference<MethodReference>() ?: return@firstNotNullOfOrNull null
            if (reference.definingClass != "Landroid/content/Intent;" ||
                reference.name != "<init>" ||
                reference.parameterTypeNames != listOf("Landroid/content/Context;", "Ljava/lang/Class;")
            ) {
                return@firstNotNullOfOrNull null
            }

            instruction.argumentRegister(0)
        } ?: throw PatchException("Could not resolve the settings context register.")

        val settingsListAddIndex =
            (settingsModelConstructorIndex until setupSettingsItemMethod.instructions.size).firstOrNull { index ->
                val instruction = setupSettingsItemMethod.instructions[index]
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) {
                    return@firstOrNull false
                }

                val reference = instruction.getReference<MethodReference>() ?: return@firstOrNull false
                reference.name == "add" &&
                        reference.returnType == "Z" &&
                        reference.parameterTypeNames == listOf("Ljava/lang/Object;")
            } ?: throw PatchException("Could not find the settings list add call.")
        val settingsListAddInstruction =
            setupSettingsItemMethod.getInstruction(settingsListAddIndex) as? FiveRegisterInstruction
                ?: throw PatchException("Settings list add call is not a normal invoke instruction.")
        val settingsListAddReference =
            setupSettingsItemMethod.instructions[settingsListAddIndex].getReference<MethodReference>()
                ?: throw PatchException("Could not resolve the settings list add reference.")
        val settingsListRegister = settingsListAddInstruction.registerC

        val dividerType = setupSettingsItemMethod.instructions
            .mapNotNull { it.getReference<MethodReference>() }
            .filter { it.name == "<init>" }
            .distinctBy { it.smaliReference }
            .singleOrNull { reference ->
                reference.parameterTypeNames.isNotEmpty() &&
                        reference.parameterTypeNames.all { type ->
                            type.length == 1 || type == DEFAULT_CONSTRUCTOR_MARKER
                        }
            }?.definingClass
            ?: throw PatchException("Could not resolve the settings group divider type.")

        val separatorIndex =
            (settingsListAddIndex until setupSettingsItemMethod.instructions.size).firstOrNull { index ->
                val instruction = setupSettingsItemMethod.instructions[index]
                instruction.opcode == Opcode.NEW_INSTANCE &&
                        instruction.getReference<TypeReference>()?.type == dividerType
            } ?: throw PatchException("Could not find the divider closing the Laboratory group.")
        val originalInstruction = setupSettingsItemMethod.instructions[separatorIndex]
        val originalNewInstanceRegister = (originalInstruction as OneRegisterInstruction).registerA

        val themePrefClass = ThemePrefNightModeReadFingerprint.classDef
        val themePrefInstanceField = themePrefClass.fields.singleOrNull {
            it.type == themePrefClass.type &&
                    it.accessFlags and AccessFlags.STATIC.value != 0
        } ?: throw PatchException("Could not find ThemePref singleton field")
        val themePrefNightModeReader = ThemePrefNightModeReadFingerprint.method

        SetupSettingsItemFingerprint.classDef.methods.add(
            addMorpheSettingsItemMethod(
                definingClass = setupSettingsItemMethod.definingClass,
                mainSettingItemType = mainSettingItemTypeClass.type,
                finishSetupSettingsModel = finishSetupSettingsModel,
                initialSettingsItemType = initialSettingsItemType,
                initialSettingsItemReference = initialSettingsItemReference,
                settingsItemHostField = settingsItemHostField,
                trackingActionReference = trackingActionReference,
                trackingActionArgument = trackingActionArgument,
                settingsListAddReference = settingsListAddReference,
                themePrefType = themePrefClass.type,
                themePrefInstanceField = themePrefInstanceField,
                themePrefNightModeReader = themePrefNightModeReader,
            )
        )

        val helperCallRegisterProvider = setupSettingsItemMethod.getFreeRegisterProvider(
            separatorIndex,
            3,
            contextRegister,
            trackingActionReceiverRegister,
            settingsListRegister,
            originalNewInstanceRegister,
        )
        val contextArgumentRegister = helperCallRegisterProvider.getFreeRegister4Bit()
        val trackingActionArgumentRegister = helperCallRegisterProvider.getFreeRegister4Bit()
        val settingsListArgumentRegister = helperCallRegisterProvider.getFreeRegister4Bit()

        setupSettingsItemMethod.replaceInstruction(separatorIndex, "nop")
        setupSettingsItemMethod.addInstructions(
            separatorIndex + 1,
            """
                move-object/from16 v$contextArgumentRegister, v$contextRegister
                move-object/from16 v$trackingActionArgumentRegister, v$trackingActionReceiverRegister
                move-object/from16 v$settingsListArgumentRegister, v$settingsListRegister
                invoke-static {v$contextArgumentRegister, v$trackingActionArgumentRegister, v$settingsListArgumentRegister}, ${setupSettingsItemMethod.definingClass}->$ADD_MORPHE_SETTINGS_ITEM_METHOD(Landroid/content/Context;${trackingActionReference.definingClass}${settingsListAddReference.definingClass})V
                new-instance v$originalNewInstanceRegister, $dividerType # stub
            """.trimIndent()
        )
    }
}

private fun addMorpheSettingsItemMethod(
    definingClass: String,
    mainSettingItemType: String,
    finishSetupSettingsModel: MethodReference,
    initialSettingsItemType: String,
    initialSettingsItemReference: MethodReference,
    settingsItemHostField: FieldReference,
    trackingActionReference: MethodReference,
    trackingActionArgument: Int,
    settingsListAddReference: MethodReference,
    themePrefType: String,
    themePrefInstanceField: FieldReference,
    themePrefNightModeReader: MethodReference,
): MutableMethod = ImmutableMethod(
    definingClass,
    ADD_MORPHE_SETTINGS_ITEM_METHOD,
    listOf(
        ImmutableMethodParameter("Landroid/content/Context;", null, null),
        ImmutableMethodParameter(trackingActionReference.definingClass, null, null),
        ImmutableMethodParameter(settingsListAddReference.definingClass, null, null),
    ),
    "V",
    AccessFlags.PRIVATE.value or AccessFlags.STATIC.value or AccessFlags.FINAL.value,
    null,
    null,
    MutableMethodImplementation(9),
).toMutable().apply {
    addInstructions(
        0,
        """
            sget-object v0, $themePrefType->${themePrefInstanceField.name}:$themePrefType
            invoke-virtual {v0}, $themePrefType->${themePrefNightModeReader.name}()I
            sget-object v0, $mainSettingItemType->MORPHE:$mainSettingItemType
            invoke-virtual {v0}, $mainSettingItemType->getStringResId()I
            move-result v1
            invoke-virtual {p0, v1}, Landroid/content/Context;->getString(I)Ljava/lang/String;
            move-result-object v1
            new-instance v2, Landroid/content/Intent;
            const-class v3, Lapp/revanced/extension/kakaotalk/settings/SettingsActivity;
            invoke-direct {v2, p0, v3}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
            const/16 v3, 0x${trackingActionArgument.toString(16)}
            invoke-virtual {p1, v3}, $trackingActionReference
            move-result-object v3
            sget-object v4, ${settingsItemHostField.definingClass}->${settingsItemHostField.name}:${settingsItemHostField.type}
            new-instance v5, $initialSettingsItemType
            invoke-direct {v5, v1, v2, v3, v4}, $initialSettingsItemReference
            new-instance v0, ${finishSetupSettingsModel.definingClass}
            sget-object v1, $mainSettingItemType->MORPHE:$mainSettingItemType
            const/4 v2, 0x0
            move-object v3, v5
            const/4 v4, 0x2
            const/4 v5, 0x0
            invoke-direct/range {v0 .. v5}, $finishSetupSettingsModel
            invoke-virtual {p2, v0}, $settingsListAddReference
            return-void
        """.trimIndent()
    )
}

private fun BytecodePatchContext.syncThemeNightModePreference() {
    ThemePrefNightModeReadFingerprint.method.apply {
        val returnIndex = instructions.indexOfLast {
            it.opcode == Opcode.RETURN
        }
        if (returnIndex < 0) {
            throw PatchException("Could not find ThemePref night mode return")
        }

        val returnRegister = (getInstruction(returnIndex) as? OneRegisterInstruction)?.registerA
            ?: throw PatchException("Could not read ThemePref night mode return register")

        addInstructions(
            returnIndex,
            """
                invoke-static {v$returnRegister}, Lapp/revanced/extension/kakaotalk/settings/KakaoThemeSettings;->setNightMode(I)V
            """.trimIndent()
        )
    }

    ThemePrefNightModeWriteFingerprint.method.addInstructions(
        0,
        """
            invoke-static {p1}, Lapp/revanced/extension/kakaotalk/settings/KakaoThemeSettings;->setNightMode(I)V
        """.trimIndent()
    )
}
