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
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.localRegisterCount
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val SETTINGS_MODEL_REGISTER_COUNT = 6
private const val MIN_SETTINGS_MODEL_REGISTER_START = 16

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
                MutableMethodImplementation(24),
            ).toMutable().apply {
                addInstructions(valuesMethod.instructions)

                val languageIndex = instructions.indexOfLast {
                    it.opcode == Opcode.SGET_OBJECT &&
                            (it as ReferenceInstruction).reference.toString().contains("LANGUAGE")
                }

                addInstructions(languageIndex + 1, """
                    sget-object v22, ${mainSettingItemTypeClass.type}->MORPHE:${mainSettingItemTypeClass.type}
                """)

                val arrayIndex = instructions.indexOfFirst {
                    it.opcode == Opcode.FILLED_NEW_ARRAY_RANGE
                }

                replaceInstruction(
                    arrayIndex,
                    "filled-new-array/range {v1 .. v22}, [${mainSettingItemTypeClass.type}"
                )
            }
        )

        val clinitMethod = mainSettingItemTypeClass.methods.find { it.name == "<clinit>" }
            ?: throw PatchException("Could not find <clinit> method")

        val insertIndex = clinitMethod.instructions.indexOfLast {
            it.opcode == Opcode.SPUT_OBJECT &&
                    it.getReference<FieldReference>()?.name == "LANGUAGE"
        } + 1

        clinitMethod.addInstructions(insertIndex, """
            new-instance v0, ${mainSettingItemTypeClass.type}
            const-string v1, "MORPHE"
            const/16 v2, 0x15
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
        val initialSettingsItemType = finishSetupSettingsModel.parameterTypes.getOrNull(2)
            ?: throw PatchException("Could not resolve initial settings item type.")

        val lastNewInstanceIndex = setupSettingsItemMethod.instructions.indexOfLast {
            it.opcode == Opcode.NEW_INSTANCE
        }
        if (lastNewInstanceIndex < 1) {
            throw PatchException("Could not find initial settings item constructor.")
        }
        val initialSettingsItemInstruction = setupSettingsItemMethod.getInstruction(lastNewInstanceIndex - 1) as BuilderInstruction35c
        val initialSettingsItemReference = initialSettingsItemInstruction.getReference<MethodReference>()
            ?: throw PatchException("Could not resolve initial settings item constructor.")

        val trackingAction = setupSettingsItemMethod.instructions.first {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                    it.getReference<MethodReference>()?.name == "action"
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

        // Keep the constructor argument range in high local registers. The host method keeps
        // context/list/tracking values in low registers around this insertion point.
        val settingsModelRegister = setupSettingsItemMethod.settingsModelRegisterRange().first
        val settingsItemTypeRegister = settingsModelRegister + 1
        val firstDefaultRegister = settingsModelRegister + 2
        val settingsItemRegister = settingsModelRegister + 3
        val flagsRegister = settingsModelRegister + 4
        val secondDefaultRegister = settingsModelRegister + 5

        setupSettingsItemMethod.addInstructions(
            separatorIndex + 1,
            """
                sget-object v$settingsModelRegister, ${themePrefClass.type}->${themePrefInstanceField.name}:${themePrefClass.type}
                invoke-virtual/range {v$settingsModelRegister .. v$settingsModelRegister}, ${themePrefClass.type}->${themePrefNightModeReader.name}()I
                new-instance v$settingsModelRegister, ${finishSetupSettingsModel.definingClass}
                sget-object v$settingsItemTypeRegister, ${mainSettingItemTypeClass.type}->MORPHE:${mainSettingItemTypeClass.type}
                new-instance v3, $initialSettingsItemType
                invoke-virtual/range {v$settingsItemTypeRegister .. v$settingsItemTypeRegister}, ${mainSettingItemTypeClass.type}->getStringResId()I
                move-result v4
                invoke-virtual {v1, v4}, Landroid/content/Context;->getString(I)Ljava/lang/String;
                move-result-object v4
                new-instance v10, Landroid/content/Intent;
                const-class v11, Lapp/revanced/extension/kakaotalk/settings/SettingsActivity;
                invoke-direct {v10, v1, v11}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                const/16 v11, 0x1e
                invoke-virtual {v9, v11}, ${trackingAction.getReference<MethodReference>()}
                move-result-object v11
                sget-object v13, Lcom/kakao/talk/activity/setting/laboratory/LaboratoryActivity;->O:Lcom/kakao/talk/activity/setting/laboratory/LaboratoryActivity${'$'}a;
                invoke-direct {v3, v4, v10, v11, v13}, $initialSettingsItemReference
                const/16 v$flagsRegister, 0x2
                const/16 v$secondDefaultRegister, 0x0
                const/16 v$firstDefaultRegister, 0x0
                move-object/from16 v$settingsItemRegister, v3
                invoke-direct/range {v$settingsModelRegister .. v$secondDefaultRegister}, $finishSetupSettingsModel
                move-object/from16 v3, v$settingsModelRegister
                invoke-virtual {v7, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                new-instance v$originalNewInstanceRegister, $originalNewInstanceType # stub
            """.trimIndent()
        )
    }
}

private fun MutableMethod.settingsModelRegisterRange(): IntRange {
    val rangeStart = localRegisterCount - SETTINGS_MODEL_REGISTER_COUNT
    if (rangeStart < MIN_SETTINGS_MODEL_REGISTER_START) {
        throw PatchException("Could not reserve high local registers for settings model injection.")
    }

    return rangeStart until rangeStart + SETTINGS_MODEL_REGISTER_COUNT
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
