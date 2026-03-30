package app.revanced.patches.kakaotalk.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
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
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

@Suppress("unused")
val addSettingsTabPatch = bytecodePatch(
    name = "Add settings tab",
    description = "Adds a settings tab to the app.",
    default = false // Since the settings-related activity has not yet been developed, it will be disabled for the time being.
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(
        addExtensionPatch,
        addResourcesPatch,
        sharedExtensionPatch
    )

    execute {
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
            const-string v4, "setting_ico_testroom"
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
        val finishSetupSettingsModel = (setupSettingsItemMethod.getInstruction(sgetCallIndex - 5) as BuilderInstruction3rc).getReference<MethodReference>()

        val lastNewInstanceIndex = setupSettingsItemMethod.instructions.indexOfLast {
            it.opcode == Opcode.NEW_INSTANCE
        }
        val initialSettingsItemInstruction = setupSettingsItemMethod.getInstruction(lastNewInstanceIndex - 1) as BuilderInstruction35c
        val initialSettingsItemReference = initialSettingsItemInstruction.getReference<MethodReference>()

        val trackingAction = setupSettingsItemMethod.instructions.first {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                    it.getReference<MethodReference>()?.name == "action"
        }

        val originalInstruction = setupSettingsItemMethod.instructions[separatorIndex]
        setupSettingsItemMethod.replaceInstruction(separatorIndex, "nop")

        setupSettingsItemMethod.addInstructions(
            separatorIndex + 1,
            """
                new-instance v15, ${finishSetupSettingsModel?.definingClass}
                sget-object v16, ${mainSettingItemTypeClass.type}->MORPHE:${mainSettingItemTypeClass.type}
                new-instance v4, ${finishSetupSettingsModel?.parameterTypes[2]}
                invoke-virtual/range {v16 .. v16}, ${mainSettingItemTypeClass.type}->getStringResId()I
                move-result v5
                invoke-virtual {v0, v5}, Landroid/content/Context;->getString(I)Ljava/lang/String;
                move-result-object v5
                new-instance v9, Landroid/content/Intent;
                const-class v12, Lcom/kakao/talk/activity/setting/laboratory/LaboratoryActivity;
                invoke-direct {v9, v0, v12}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                const/16 v12, 0x1e
                invoke-virtual {v10, v12}, ${trackingAction.getReference<MethodReference>()}
                move-result-object v12
                sget-object v14, Lcom/kakao/talk/activity/setting/laboratory/LaboratoryActivity;->Q:Lcom/kakao/talk/activity/setting/laboratory/LaboratoryActivity${'$'}a;
                invoke-direct {v4, v5, v9, v12, v14}, $initialSettingsItemReference
                const/16 v19, 0x2
                const/16 v20, 0x0
                const/16 v17, 0x0
                move-object/from16 v18, v4
                invoke-direct/range {v15 .. v20}, $finishSetupSettingsModel
                invoke-virtual {v6, v15}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                new-instance v14, ${originalInstruction.getReference<TypeReference>()?.type} # stub
            """.trimIndent()
        )
    }
}
