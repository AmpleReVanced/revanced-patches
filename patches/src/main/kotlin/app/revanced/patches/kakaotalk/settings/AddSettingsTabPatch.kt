package app.revanced.patches.kakaotalk.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

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

        val insertIndex = clinitMethod.instructions.indexOfLast {
            it.opcode == Opcode.SPUT_OBJECT &&
                    it.getReference<FieldReference>()?.name == "LANGUAGE"
        }.takeIf { it >= 0 }
            ?: throw PatchException("Could not find LANGUAGE enum initialization.")

        clinitMethod.addInstructions(insertIndex + 1, """
            new-instance v0, ${mainSettingItemTypeClass.type}
            const-string v1, "MORPHE"
            const/16 v2, 0x${morpheOrdinal.toString(16)}
            const-string v3, "morphe_label_for_ample_settings"
            const-string v4, "string"
            invoke-static {v4, v3}, Lapp/revanced/extension/kakaotalk/helper/ResourceHelper;->getResourceId(Ljava/lang/String;Ljava/lang/String;)I
            move-result v3
            const-string v4, "morphe_settings_icon_dynamic"
            const-string v5, "drawable"
            invoke-static {v5, v4}, Lapp/revanced/extension/kakaotalk/helper/ResourceHelper;->getResourceId(Ljava/lang/String;Ljava/lang/String;)I
            move-result v4
            invoke-direct {v0, v1, v2, v3, v4}, ${mainSettingItemTypeClass.type}-><init>(Ljava/lang/String;III)V
            sput-object v0, ${mainSettingItemTypeClass.type}->MORPHE:${mainSettingItemTypeClass.type}
        """)

        val setupSettingsItemMethod = SetupSettingsItemFingerprint.method

        var separatorIndex = -1
        for (i in 0 until setupSettingsItemMethod.instructions.size - 1) {
            val instruction = setupSettingsItemMethod.instructions[i]
            val nextInstruction = setupSettingsItemMethod.instructions[i + 1]

            if (instruction.opcode == Opcode.NEW_INSTANCE &&
                nextInstruction.opcode == Opcode.INVOKE_STATIC &&
                nextInstruction.getReference<MethodReference>()?.name == "getSystem" &&
                nextInstruction.getReference<MethodReference>()?.definingClass == "Landroid/content/res/Resources;") {
                separatorIndex = i
                break
            }
        }

        if (separatorIndex == -1) {
            throw PatchException("Could not find separator insertion point")
        }

        val sgetCallIndex = setupSettingsItemMethod.instructions.indexOfFirst {
            it.opcode == Opcode.SGET_OBJECT &&
                    it.getReference<FieldReference>()?.name == "CALL"
        }
        if (sgetCallIndex < 6) {
            throw PatchException("Could not find settings model constructor.")
        }
        val finishSetupSettingsModel =
            (setupSettingsItemMethod.getInstruction(sgetCallIndex - 6) as BuilderInstruction3rc)
                .getReference<MethodReference>()
                ?: throw PatchException("Could not resolve settings model constructor.")
        val initialSettingsItemType = finishSetupSettingsModel.parameterTypes.getOrNull(2)?.toString()
            ?: throw PatchException("Could not resolve initial settings item type.")
        val finishSetupSettingsModelParameterTypes = finishSetupSettingsModel.parameterTypes.map { it.toString() }
        val settingsModelConstructorIndex = (separatorIndex - 1 downTo 0).firstOrNull { index ->
            val instruction = setupSettingsItemMethod.instructions[index]
            if (instruction.opcode != Opcode.INVOKE_DIRECT_RANGE) {
                return@firstOrNull false
            }

            val reference = instruction.getReference<MethodReference>() ?: return@firstOrNull false
            reference.definingClass == finishSetupSettingsModel.definingClass &&
                    reference.name == finishSetupSettingsModel.name &&
                    reference.returnType == finishSetupSettingsModel.returnType &&
                    reference.parameterTypes.map { it.toString() } == finishSetupSettingsModelParameterTypes
        } ?: throw PatchException("Could not find existing settings model constructor.")
        val settingsModelConstructorInstruction =
            setupSettingsItemMethod.getInstruction(settingsModelConstructorIndex) as? BuilderInstruction3rc
                ?: throw PatchException("Existing settings model constructor is not a range instruction.")
        val settingsModelRegister = settingsModelConstructorInstruction.startRegister
        val settingsItemTypeRegister = settingsModelRegister + 1
        val firstDefaultRegister = settingsModelRegister + 2
        val settingsItemRegister = settingsModelRegister + 3
        val flagsRegister = settingsModelRegister + 4
        val secondDefaultRegister = settingsModelRegister + 5

        val laboratoryTypeIndex = (0 until settingsModelConstructorIndex).lastOrNull { index ->
            val instruction = setupSettingsItemMethod.instructions[index]
            if (instruction.opcode != Opcode.SGET_OBJECT) {
                return@lastOrNull false
            }

            val reference = instruction.getReference<FieldReference>() ?: return@lastOrNull false
            reference.definingClass == mainSettingItemTypeClass.type && reference.name == "LABORATORY"
        } ?: throw PatchException("Could not find LABORATORY settings item.")
        val initialSettingsItemIndex = (laboratoryTypeIndex until settingsModelConstructorIndex).firstOrNull { index ->
            val instruction = setupSettingsItemMethod.instructions[index]
            if (instruction.opcode != Opcode.INVOKE_DIRECT) {
                return@firstOrNull false
            }

            val reference = instruction.getReference<MethodReference>() ?: return@firstOrNull false
            reference.definingClass == initialSettingsItemType && reference.name == "<init>"
        } ?: throw PatchException("Could not find initial settings item constructor.")
        val initialSettingsItemInstruction =
            setupSettingsItemMethod.getInstruction(initialSettingsItemIndex) as? BuilderInstruction35c
                ?: throw PatchException("Initial settings item constructor is not a normal invoke instruction.")
        val initialSettingsItemReference = initialSettingsItemInstruction.getReference<MethodReference>()
            ?: throw PatchException("Could not resolve initial settings item constructor.")
        val itemRegister = initialSettingsItemInstruction.registerC
        val itemTitleRegister = initialSettingsItemInstruction.registerD
        val itemIntentRegister = initialSettingsItemInstruction.registerE
        val itemActionRegister = initialSettingsItemInstruction.registerF
        val laboratoryActivityRegister = initialSettingsItemInstruction.registerG

        val settingsItemActionType = initialSettingsItemReference.parameterTypes.getOrNull(2)?.toString()
            ?: throw PatchException("Could not resolve settings item action type.")
        val trackingActionIndex = (initialSettingsItemIndex - 1 downTo laboratoryTypeIndex).firstOrNull { index ->
            val instruction = setupSettingsItemMethod.instructions[index]
            if (instruction.opcode != Opcode.INVOKE_VIRTUAL) {
                return@firstOrNull false
            }

            val reference = instruction.getReference<MethodReference>() ?: return@firstOrNull false
            reference.name == "action" &&
                    reference.returnType == settingsItemActionType &&
                    reference.parameterTypes.size == 1 &&
                    reference.parameterTypes.first().toString() == "I"
        } ?: throw PatchException("Could not find tracking action call.")
        val trackingActionInstruction =
            setupSettingsItemMethod.getInstruction(trackingActionIndex) as? FiveRegisterInstruction
                ?: throw PatchException("Tracking action call is not a normal invoke instruction.")
        val trackingActionReference = setupSettingsItemMethod.instructions[trackingActionIndex].getReference<MethodReference>()
            ?: throw PatchException("Could not resolve tracking action reference.")
        val trackingActionReceiverRegister = trackingActionInstruction.registerC

        val settingsListAddIndex = (separatorIndex - 1 downTo 0).firstOrNull { index ->
            val instruction = setupSettingsItemMethod.instructions[index]
            if (instruction.opcode != Opcode.INVOKE_VIRTUAL) {
                return@firstOrNull false
            }

            val reference = instruction.getReference<MethodReference>() ?: return@firstOrNull false
            reference.name == "add" &&
                    reference.returnType == "Z" &&
                    reference.parameterTypes.size == 1 &&
                    reference.parameterTypes.first().toString() == "Ljava/lang/Object;" &&
                    reference.definingClass in listOf("Ljava/util/ArrayList;", "Ljava/util/List;")
        } ?: throw PatchException("Could not find settings list add call.")
        val settingsListAddInstruction =
            setupSettingsItemMethod.getInstruction(settingsListAddIndex) as? FiveRegisterInstruction
                ?: throw PatchException("Settings list add call is not a normal invoke instruction.")
        val settingsListAddReference = setupSettingsItemMethod.instructions[settingsListAddIndex].getReference<MethodReference>()
            ?: throw PatchException("Could not resolve settings list add reference.")
        val settingsListRegister = settingsListAddInstruction.registerC
        val contextParameterRegister = if (AccessFlags.STATIC.isSet(setupSettingsItemMethod.accessFlags)) {
            "p0"
        } else {
            "p1"
        }

        val originalInstruction = setupSettingsItemMethod.instructions[separatorIndex]
        val originalNewInstanceRegister = (originalInstruction as? OneRegisterInstruction)?.registerA
            ?: throw PatchException("Could not read separator new-instance register.")
        val originalNewInstanceType = originalInstruction.getReference<TypeReference>()?.type
            ?: throw PatchException("Could not read separator new-instance type.")
        setupSettingsItemMethod.replaceInstruction(separatorIndex, "nop")

        val themePrefClass = ThemePrefNightModeReadFingerprint.classDef
        val themePrefInstanceField = themePrefClass.fields.firstOrNull {
            it.type == themePrefClass.type &&
                    it.accessFlags and AccessFlags.STATIC.value != 0
        } ?: throw PatchException("Could not find ThemePref singleton field")
        val themePrefNightModeReader = ThemePrefNightModeReadFingerprint.method

        setupSettingsItemMethod.addInstructions(
            separatorIndex + 1,
            """
                sget-object v$settingsModelRegister, ${themePrefClass.type}->${themePrefInstanceField.name}:${themePrefClass.type}
                invoke-virtual/range {v$settingsModelRegister .. v$settingsModelRegister}, ${themePrefClass.type}->${themePrefNightModeReader.name}()I
                new-instance v$settingsModelRegister, ${finishSetupSettingsModel.definingClass}
                sget-object v$settingsItemTypeRegister, ${mainSettingItemTypeClass.type}->MORPHE:${mainSettingItemTypeClass.type}
                invoke-virtual/range {v$settingsItemTypeRegister .. v$settingsItemTypeRegister}, ${mainSettingItemTypeClass.type}->getStringResId()I
                move-result v$itemTitleRegister
                move-object/from16 v$itemRegister, $contextParameterRegister
                invoke-virtual {v$itemRegister, v$itemTitleRegister}, Landroid/content/Context;->getString(I)Ljava/lang/String;
                move-result-object v$itemTitleRegister
                new-instance v$itemRegister, $initialSettingsItemType
                new-instance v$itemIntentRegister, Landroid/content/Intent;
                move-object/from16 v$itemActionRegister, $contextParameterRegister
                const-class v$laboratoryActivityRegister, Lapp/revanced/extension/kakaotalk/settings/SettingsActivity;
                invoke-direct {v$itemIntentRegister, v$itemActionRegister, v$laboratoryActivityRegister}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                const/16 v$itemActionRegister, 0x1e
                invoke-virtual {v$trackingActionReceiverRegister, v$itemActionRegister}, $trackingActionReference
                move-result-object v$itemActionRegister
                sget-object v$laboratoryActivityRegister, Lcom/kakao/talk/activity/setting/laboratory/LaboratoryActivity;->O:Lcom/kakao/talk/activity/setting/laboratory/LaboratoryActivity${'$'}a;
                invoke-direct {v$itemRegister, v$itemTitleRegister, v$itemIntentRegister, v$itemActionRegister, v$laboratoryActivityRegister}, $initialSettingsItemReference
                const/16 v$flagsRegister, 0x2
                const/16 v$secondDefaultRegister, 0x0
                const/16 v$firstDefaultRegister, 0x0
                move-object/from16 v$settingsItemRegister, v$itemRegister
                invoke-direct/range {v$settingsModelRegister .. v$secondDefaultRegister}, $finishSetupSettingsModel
                move-object/from16 v$firstDefaultRegister, v$settingsListRegister
                move-object/from16 v$settingsItemRegister, v$settingsModelRegister
                invoke-virtual/range {v$firstDefaultRegister .. v$settingsItemRegister}, $settingsListAddReference
                new-instance v$originalNewInstanceRegister, $originalNewInstanceType # stub
            """.trimIndent()
        )
    }
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
