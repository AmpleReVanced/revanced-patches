package app.revanced.patches.kakaotalk.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tab.fingerprints.ChooseOpenLinkTabFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.ChooseNowChildTabFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.GetOpenLinkModuleFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.NowChildTabFromNameFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.NowChildTabFromPositionFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.NowFragmentOnViewCreatedFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.NowTabPagerAdapterFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.TransitionOpenLinkOrShortformMethodFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/RemoveShortFormTabPatch;"

@Suppress("unused")
val removeShortFormTabPatch = bytecodePatch(
    name = "Remove Short-form Tab",
    description = "Removes the Short-form tab from the now fragment.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.NAVIGATION.addPreferences(
            SwitchPreference(
                key = "morphe_pref_remove_short_form_tab",
                titleKey = "morphe_settings_catalog_remove_short_form_tab",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val onViewCreated = NowFragmentOnViewCreatedFingerprint.method
        val nowTabChips = onViewCreated.instructions
            .asSequence()
            .filterIsInstance<BuilderInstruction22c>()
            .filter {
                it.opcode == Opcode.IGET_OBJECT &&
                        it.getReference<FieldReference>()?.type == "Lcom/kakao/talk/core/ui/widget/TdChip;"
            }
            .distinctBy {
                val reference = it.getReference<FieldReference>()
                    ?: throw PatchException("Could not inspect Now tab chip field")
                reference.definingClass to reference.name
            }
            .toList()
        val hiddenNowTabChips = listOfNotNull(
            nowTabChips.getOrNull(0),
            nowTabChips.getOrNull(2),
        )
        if (hiddenNowTabChips.size != 2) {
            throw PatchException("Could not find Short-form and Brand chips in NowFragment.onViewCreated")
        }

        hiddenNowTabChips.asReversed().forEachIndexed { index, chip ->
            val label = "morphe_keep_now_tab_chip_$index"

            onViewCreated.addInstructionsWithLabels(
                onViewCreated.instructions.indexOf(chip) + 1,
                """
                    invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                    move-result v3
                    if-eqz v3, :$label
                    const/16 v3, 0x8
                    invoke-virtual {p1, v3}, Landroid/view/View;->setVisibility(I)V
                    :$label
                    nop
                """.trimIndent()
            )
        }

        val getChildTab = onViewCreated.instructions.lastOrNull { it.opcode == Opcode.CHECK_CAST } as? BuilderInstruction21c
            ?: throw PatchException("Could not find current tab cast in NowFragment.onViewCreated")
        val fieldRef = getChildTab.getReference<TypeReference>()
            ?: throw PatchException("Could not infer now child tab enum type")

        onViewCreated.addInstructionsWithLabels(
            onViewCreated.instructions.indexOfLast { it.opcode == Opcode.MOVE_RESULT_OBJECT } + 1,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result v1
                if-eqz v1, :morphe_keep_selected_tab
                sget-object v0, ${fieldRef.type}->Openlink:${fieldRef.type}
                :morphe_keep_selected_tab
                nop
            """.trimIndent()
        )

        val getItemCountMethod = NowTabPagerAdapterFingerprint.classDef.methods.firstOrNull {
            it.name == "getItemCount"
        } ?: throw PatchException("Could not find NowTabPagerAdapter.getItemCount")

        getItemCountMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result p0
                if-eqz p0, :morphe_original_item_count
                const/4 p0, 0x1
                return p0
                :morphe_original_item_count
                nop
            """.trimIndent()
        )

        val createFragmentMethod = NowTabPagerAdapterFingerprint.method
        val getOpenLinkModuleMethod = GetOpenLinkModuleFingerprint.method
        fun nowChildTabField(name: String) = createFragmentMethod.instructions.firstOrNull {
            val reference = it.getReference<FieldReference>()

            it.opcode == Opcode.SGET_OBJECT &&
                    reference?.definingClass == fieldRef.type &&
                    reference.name == name
        }?.getReference<FieldReference>()
            ?: throw PatchException("Could not find $name field in NowTabPagerAdapter.w")

        val openLinkField = nowChildTabField("Openlink")
        val shortFormField = nowChildTabField("ShortForm")
        val brandField = nowChildTabField("Brand")
        val getPositionMethod = createFragmentMethod.instructions.firstOrNull {
            val reference = it.getReference<MethodReference>()

            it.opcode == Opcode.INVOKE_VIRTUAL &&
                    reference?.definingClass == fieldRef.type &&
                    reference.name == "getPosition" &&
                    reference.returnType == "I" &&
                    reference.parameterTypes.isEmpty()
            }?.getReference<MethodReference>()
            ?: throw PatchException("Could not find getPosition()I call in NowTabPagerAdapter.w")

        NowChildTabFromPositionFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result v0
                if-eqz v0, :morphe_keep_now_child_tab_from_position
                if-eqz p1, :morphe_keep_now_child_tab_from_position
                invoke-virtual {p1}, Ljava/lang/Integer;->intValue()I
                move-result v0
                sget-object v1, $shortFormField
                invoke-virtual {v1}, $getPositionMethod
                move-result v1
                if-eq v0, v1, :morphe_force_openlink_child_tab_from_position
                sget-object v1, $brandField
                invoke-virtual {v1}, $getPositionMethod
                move-result v1
                if-ne v0, v1, :morphe_keep_now_child_tab_from_position
                :morphe_force_openlink_child_tab_from_position
                sget-object v0, $openLinkField
                return-object v0
                :morphe_keep_now_child_tab_from_position
                nop
            """.trimIndent()
        )

        NowChildTabFromNameFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result p0
                if-eqz p0, :morphe_keep_now_child_tab_from_name
                if-eqz p1, :morphe_keep_now_child_tab_from_name
                const-string p0, "brand"
                invoke-virtual {p1, p0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
                move-result p0
                if-nez p0, :morphe_force_openlink_child_tab_from_name
                const-string p0, "shortform"
                invoke-virtual {p1, p0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
                move-result p0
                if-eqz p0, :morphe_keep_now_child_tab_from_name
                :morphe_force_openlink_child_tab_from_name
                sget-object p0, $openLinkField
                return-object p0
                :morphe_keep_now_child_tab_from_name
                nop
            """.trimIndent()
        )

        createFragmentMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result v0
                if-eqz v0, :morphe_original_fragment
                invoke-static {}, ${getOpenLinkModuleMethod.definingClass}->${getOpenLinkModuleMethod.name}()Lcom/kakao/talk/module/openlink/contract/OpenLinkModuleFacade;
                move-result-object v0
                invoke-interface {v0}, Lcom/kakao/talk/module/openlink/contract/OpenLinkModuleFacade;->createOpenChatTabFragment()Landroidx/fragment/app/Fragment;
                move-result-object v0
                return-object v0
                :morphe_original_fragment
                nop
            """.trimIndent()
        )

        val getItemIdMethod = NowTabPagerAdapterFingerprint.classDef.methods.firstOrNull {
            it.returnType == "J" && it.parameterTypes == listOf("I")
        } ?: throw PatchException("Could not find NowTabPagerAdapter.getItemId")

        getItemIdMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result p0
                if-eqz p0, :morphe_original_item_id
                sget-object p0, $openLinkField
                invoke-virtual {p0}, $getPositionMethod
                move-result p0
                int-to-long p0, p0
                return-wide p0
                :morphe_original_item_id
                nop
            """.trimIndent()
        )

        val containsItemMethod = NowTabPagerAdapterFingerprint.classDef.methods.firstOrNull {
            it.returnType == "Z" && it.parameterTypes == listOf("J")
        } ?: throw PatchException("Could not find NowTabPagerAdapter.containsItem")

        containsItemMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result v0
                if-eqz v0, :morphe_original_contains_item
                sget-object v0, $openLinkField
                invoke-virtual {v0}, $getPositionMethod
                move-result v0
                int-to-long v0, v0
                cmp-long v0, p1, v0
                if-nez v0, :morphe_removed_short_form_item
                const/4 v0, 0x1
                return v0
                :morphe_removed_short_form_item
                const/4 v0, 0x0
                return v0
                :morphe_original_contains_item
                nop
            """.trimIndent()
        )

        TransitionOpenLinkOrShortformMethodFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result v0
                if-eqz v0, :morphe_original_transition
                return-void
                :morphe_original_transition
                nop
            """.trimIndent()
        )

        val chooseOpenLinkTabMethod = ChooseOpenLinkTabFingerprint.method
        val openLinkPositionIdx = ChooseOpenLinkTabFingerprint.instructionMatches[1].index

        val openLinkPositionRegister =
            (chooseOpenLinkTabMethod.getInstruction(openLinkPositionIdx + 1) as? OneRegisterInstruction)
                ?.registerA
                ?: throw PatchException("Could not find Openlink getPosition()I move-result in chooseOpenLinkTab")
        val openLinkPositionMethodRef =
            chooseOpenLinkTabMethod.getInstruction(openLinkPositionIdx)
                .getReference<MethodReference>()
                ?: throw PatchException("Could not find Openlink getPosition()I reference in chooseOpenLinkTab")
        if (openLinkPositionMethodRef.definingClass != fieldRef.type) {
            throw PatchException("Openlink getPosition()I belongs to unexpected enum type")
        }
        val openLinkTabRegister = when (val invokeInsn = chooseOpenLinkTabMethod.getInstruction(openLinkPositionIdx)) {
            is BuilderInstruction35c -> invokeInsn.registerC
            is BuilderInstruction3rc -> invokeInsn.startRegister
            else -> throw PatchException("Unsupported invoke instruction type in chooseOpenLinkTab: ${invokeInsn::class.java.name}")
        }

        chooseOpenLinkTabMethod.removeInstructions(openLinkPositionIdx, 2)
        chooseOpenLinkTabMethod.addInstructionsWithLabels(
            openLinkPositionIdx,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result v$openLinkPositionRegister
                if-eqz v$openLinkPositionRegister, :morphe_keep_openlink_tab_position
                const/4 v$openLinkPositionRegister, 0x0
                goto :morphe_after_openlink_tab_position
                :morphe_keep_openlink_tab_position
                invoke-virtual {v$openLinkTabRegister}, $openLinkPositionMethodRef
                move-result v$openLinkPositionRegister
                :morphe_after_openlink_tab_position
                nop
            """.trimIndent()
        )

        val chooseNowChildTabMethod = ChooseNowChildTabFingerprint.method
        val getPositionIdx = ChooseNowChildTabFingerprint.instructionMatches.first().index
        val getPositionResultIdx = getPositionIdx + 1
        val positionRegister = (chooseNowChildTabMethod.getInstruction(getPositionResultIdx) as? OneRegisterInstruction)
            ?.registerA
            ?: throw PatchException("Could not find getPosition()I move-result in chooseNowChildTab")
        val getPositionMethodRef = chooseNowChildTabMethod.getInstruction(getPositionIdx)
            .getReference<MethodReference>()
            ?: throw PatchException("Could not find getPosition()I reference in chooseNowChildTab")
        if (getPositionMethodRef.definingClass != fieldRef.type ||
            getPositionMethodRef.name != "getPosition" ||
            getPositionMethodRef.returnType != "I" ||
            getPositionMethodRef.parameterTypes.isNotEmpty()
        ) {
            throw PatchException("ChooseNowChildTab getPosition()I match is not the NowChildTab position accessor")
        }
        val tabRegister = when (val invokeInsn = chooseNowChildTabMethod.getInstruction(getPositionIdx)) {
            is BuilderInstruction35c -> invokeInsn.registerC
            is BuilderInstruction3rc -> invokeInsn.startRegister
            else -> throw PatchException("Unsupported invoke instruction type in chooseNowChildTab: ${invokeInsn::class.java.name}")
        }
        val flagRegister = chooseNowChildTabMethod.getFreeRegisterProvider(
            getPositionIdx,
            1,
            tabRegister
        ).getFreeRegister()

        chooseNowChildTabMethod.removeInstructions(getPositionIdx, 2)
        chooseNowChildTabMethod.addInstructionsWithLabels(
            getPositionIdx,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result v$flagRegister
                if-eqz v$flagRegister, :morphe_keep_now_child_tab
                sget-object v$tabRegister, ${fieldRef.type}->Openlink:${fieldRef.type}
                invoke-virtual {v$tabRegister}, $getPositionMethodRef
                move-result v$positionRegister
                const/4 v$positionRegister, 0x0
                goto :morphe_after_now_child_tab
                :morphe_keep_now_child_tab
                invoke-virtual {v$tabRegister}, $getPositionMethodRef
                move-result v$positionRegister
                :morphe_after_now_child_tab
                nop
            """.trimIndent()
        )
    }
}
