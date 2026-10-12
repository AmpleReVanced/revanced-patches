package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.cloneParameters
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.ChooseNowChildTabFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.ChooseOpenLinkTabFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.GetOpenLinkModuleFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.NowBrandChipFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.NowChildTabFromNameFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.NowChildTabFromPositionFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.NowTabChipStateFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.NowTabPagerAdapterFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.NowTabReselectionFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.nowChildTabObserverFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.nowChildTabSetterFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.nowTabChipFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.nowTabPageSelectionFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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

        val nowChildTabType = NowChildTabFromPositionFingerprint.method.returnType

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
                    reference?.definingClass == nowChildTabType &&
                    reference.name == name
        }?.getReference<FieldReference>()
            ?: throw PatchException("Could not find $name field in NowTabPagerAdapter")

        val openLinkField = nowChildTabField("Openlink")
        val shortFormField = nowChildTabField("ShortForm")
        val brandField = nowChildTabField("Brand")
        val getPositionMethod = createFragmentMethod.instructions.firstOrNull {
            val reference = it.getReference<MethodReference>()

            it.opcode == Opcode.INVOKE_VIRTUAL &&
                    reference?.definingClass == nowChildTabType &&
                    reference.name == "getPosition" &&
                    reference.returnType == "I" &&
                    reference.parameterTypes.isEmpty()
            }?.getReference<MethodReference>()
            ?: throw PatchException("Could not find getPosition()I call in NowTabPagerAdapter")

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

        val chipStateClass = NowTabChipStateFingerprint.classDef
        val chipTabGetter = chipStateClass.methods.single {
            it.parameterTypes.isEmpty() && it.returnType == nowChildTabType
        }
        nowTabChipFingerprint(chipStateClass.type).method.apply {
            val registers = getFreeRegisterProvider(0, 2)
            val tabRegister = registers.getFreeRegister4Bit()
            val flagRegister = registers.getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                    move-result v$flagRegister
                    if-eqz v$flagRegister, :morphe_keep_chip
                    invoke-virtual/range {p0 .. p0}, ${chipTabGetter.smaliReference}
                    move-result-object v$tabRegister
                    sget-object v$flagRegister, $openLinkField
                    if-eq v$tabRegister, v$flagRegister, :morphe_keep_chip
                    return-void
                    :morphe_keep_chip
                    nop
                """,
            )
        }
        NowBrandChipFingerprint.method.apply {
            val flagRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                    move-result v$flagRegister
                    if-eqz v$flagRegister, :morphe_keep_brand_chip
                    return-void
                    :morphe_keep_brand_chip
                    nop
                """,
            )
        }

        val chooseOpenLinkTabMethod = ChooseOpenLinkTabFingerprint.method
        val selectionFlag = chooseOpenLinkTabMethod.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        chooseOpenLinkTabMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result v$selectionFlag
                if-eqz v$selectionFlag, :morphe_keep_requested_tab
                sget-object p1, $openLinkField
                :morphe_keep_requested_tab
                nop
            """,
        )
        val chooseNowChildTabMethod = ChooseNowChildTabFingerprint.method
        val selectedTabCall = chooseNowChildTabMethod.indexOfFirstInstructionOrThrow(
            methodCall(
                parameters = listOf(chooseOpenLinkTabMethod.definingClass, nowChildTabType),
                returnType = "V",
                opcode = Opcode.INVOKE_STATIC,
            ),
        )
        val selectedTabRegister = chooseNowChildTabMethod.getInstruction<BuilderInstruction35c>(selectedTabCall).registerD
        val initialFlagRegister = chooseNowChildTabMethod.getFreeRegisterProvider(
            selectedTabCall, 1, selectedTabRegister,
        ).getFreeRegister4Bit()
        chooseNowChildTabMethod.addInstructionsWithLabels(
            selectedTabCall,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result v$initialFlagRegister
                if-eqz v$initialFlagRegister, :morphe_keep_initial_tab
                sget-object v$selectedTabRegister, $openLinkField
                :morphe_keep_initial_tab
                nop
            """,
        )
        nowTabPageSelectionFingerprint(nowChildTabType).matchAll(4 .. 4).forEach { match ->
            val index = match.instructionMatches[1].index
            val positionRegister = match.method.getInstruction<BuilderInstruction35c>(index).registerD
            match.method.addInstructions(
                index,
                """
                    invoke-static/range {v$positionRegister .. v$positionRegister}, $EXTENSION_CLASS->getPageIndex(I)I
                    move-result v$positionRegister
                """,
            )
        }

        NowTabReselectionFingerprint.method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                    move-result v$register
                    if-eqz v$register, :morphe_original_reselection
                    return-void
                    :morphe_original_reselection
                    nop
                """,
            )
        }

        fun MutableMethod.normalizeHiddenTab() {
            cloneParameters().apply {
                val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                        move-result v$register
                        if-eqz v$register, :morphe_keep_tab
                        sget-object v$register, $shortFormField
                        if-eq p1, v$register, :morphe_use_openlink
                        sget-object v$register, $brandField
                        if-ne p1, v$register, :morphe_keep_tab
                        :morphe_use_openlink
                        sget-object p1, $openLinkField
                        :morphe_keep_tab
                        nop
                    """,
                )
            }
        }

        val tabSetter = nowChildTabSetterFingerprint(nowChildTabType).method
        nowChildTabObserverFingerprint(tabSetter).matchAll(2 .. 2).forEach {
            it.method.normalizeHiddenTab()
        }
        tabSetter.normalizeHiddenTab()
    }
}
