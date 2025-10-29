package app.revanced.patches.kakaotalk.settings

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.sharedExtensionPatch
import app.revanced.patches.kakaotalk.settings.fingerprints.mainSettingItemTypeFingerprint
import app.revanced.patches.kakaotalk.settings.fingerprints.setupSettingsItemFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

@Suppress("unused")
val addSettingsTabPatch = bytecodePatch(
    name = "Add settings tab",
    description = "Adds a settings tab to the app."
) {
    compatibleWith("com.kakao.talk"("25.9.0"))
    dependsOn(
        addExtensionPatch,
        addResourcesPatch,
        sharedExtensionPatch
    )

    execute {
        addResources("kakaotalk", "settings.revancedSettingsPatch")

        val mainSettingItemTypeClass = mainSettingItemTypeFingerprint.classDef

        mainSettingItemTypeClass.fields.add(
            ImmutableField(
                mainSettingItemTypeClass.type,
                "REVANCED",
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
                MutableMethodImplementation(21),
            ).toMutable().apply {
                addInstructions(valuesMethod.instructions)

                val languageIndex = instructions.indexOfLast {
                    it.opcode == Opcode.SGET_OBJECT &&
                            (it as ReferenceInstruction).reference.toString().contains("LANGUAGE")
                }

                addInstructions(languageIndex + 1, """
                    sget-object v20, ${mainSettingItemTypeClass.type}->REVANCED:${mainSettingItemTypeClass.type}
                """)

                val arrayIndex = instructions.indexOfFirst {
                    it.opcode == Opcode.FILLED_NEW_ARRAY_RANGE
                }

                replaceInstruction(
                    arrayIndex,
                    "filled-new-array/range {v0 .. v20}, [${mainSettingItemTypeClass.type}"
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
            const-string v1, "REVANCED"
            const/16 v2, 0x14
            const-string v3, "revanced_label_for_revanced_settings"
            const-string v4, "string"
            invoke-static {v4, v3}, Lapp/revanced/extension/kakaotalk/helper/ResourceHelper;->getResourceId(Ljava/lang/String;Ljava/lang/String;)I
            move-result v3
            const-string v4, "setting_ico_testroom"
            const-string v5, "drawable"
            invoke-static {v5, v4}, Lapp/revanced/extension/kakaotalk/helper/ResourceHelper;->getResourceId(Ljava/lang/String;Ljava/lang/String;)I
            move-result v4
            invoke-direct {v0, v1, v2, v3, v4}, ${mainSettingItemTypeClass.type}-><init>(Ljava/lang/String;III)V
            sput-object v0, ${mainSettingItemTypeClass.type}->REVANCED:${mainSettingItemTypeClass.type}
        """)

        val setupSettingsItemMethod = setupSettingsItemFingerprint.method

        var separatorIndex = -1
        for (i in 0 until setupSettingsItemMethod.instructions.size - 1) {
            val instruction = setupSettingsItemMethod.instructions[i]
            val nextInstruction = setupSettingsItemMethod.instructions[i + 1]

            if (instruction.opcode == Opcode.NEW_INSTANCE &&
                (instruction as ReferenceInstruction).reference.toString() == "LYj/K;" &&
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

        setupSettingsItemMethod.replaceInstruction(separatorIndex, "nop")

        setupSettingsItemMethod.addInstructions(
            separatorIndex + 1,
            """
                new-instance v2, LYj/E0;

                .line 111
                sget-object v22, LYj/F0;->REVANCED:LYj/F0;

                .line 112
                new-instance v4, Lel/c;

                .line 113
                invoke-virtual/range {v22 .. v22}, LYj/F0;->getStringResId()I

                move-result v5

                invoke-virtual {v6, v5}, Landroid/content/Context;->getString(I)Ljava/lang/String;

                move-result-object v5

                invoke-static {v5, v9}, Lkotlin/jvm/internal/w;->i(Ljava/lang/Object;Ljava/lang/String;)V

                .line 114
                new-instance v8, Landroid/content/Intent;

                const-class v12, Lcom/kakao/talk/activity/setting/laboratory/LaboratoryActivity;

                invoke-direct {v8, v6, v12}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

                const/16 v12, 0x1e

                .line 115
                invoke-virtual {v10, v12}, LCq/a;->action(I)LCq/c;

                move-result-object v12

                .line 116
                sget-object v14, Lcom/kakao/talk/activity/setting/laboratory/LaboratoryActivity;->O:Lcom/kakao/talk/activity/setting/laboratory/LaboratoryActivity${'$'}a;

                .line 117
                invoke-direct {v4, v5, v8, v12, v14}, Lel/c;-><init>(Ljava/lang/String;Landroid/content/Intent;LCq/c;Lel/b;)V

                const/16 v25, 0x2

                const/16 v26, 0x0

                const/16 v23, 0x0

                move-object/from16 v21, v2

                move-object/from16 v24, v4

                .line 118
                invoke-direct/range {v21 .. v26}, LYj/E0;-><init>(LYj/F0;ZLel/c;ILkotlin/jvm/internal/DefaultConstructorMarker;)V

                invoke-virtual {v7, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                
                new-instance v0, LYj/K; # stub
            """.trimIndent()
        )
    }
}
