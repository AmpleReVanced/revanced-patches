package app.revanced.patches.kakaotalk.tab

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.getReference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tab.fingerprints.AddMoreTabBodySectionsFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.AddMoreTabServiceSectionsFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.MoreTabGamePlaySectionFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.MoreTabGlobalServiceGroupSectionFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.MoreTabKakaoNowSectionFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.MoreTabKakaoPaySectionFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.MoreTabLineServiceSectionFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.MoreTabServiceGroupSectionFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.MoreTabWeatherSectionFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.WeatherViewHolderBindFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.moreTabGlobalServiceGroupAdditionFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.moreTabGlobalServiceGroupViewHolderBindFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.moreTabLineServiceViewHolderBindFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.moreTabServiceGroupViewHolderBindFingerprint
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val SETTINGS_CLASS = "Lapp/revanced/extension/kakaotalk/settings/Settings;"
private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/HideMoreTabComponentsPatch;"

@Suppress("unused")
val hideMoreTabComponentsPatch = bytecodePatch(
    name = "Hide More tab components",
    description = "Adds options to hide components from the More tab.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.NAVIGATION.addPreferences(
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_kakao_pay_section",
                titleKey = "morphe_settings_catalog_hide_more_tab_kakao_pay_section",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_game_play_section",
                titleKey = "morphe_settings_catalog_hide_more_tab_game_play_section",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_kakao_now_section",
                titleKey = "morphe_settings_catalog_hide_more_tab_kakao_now_section",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_weather_section",
                titleKey = "morphe_settings_catalog_hide_more_tab_weather_section",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_service_group_section",
                titleKey = "morphe_settings_catalog_hide_more_tab_service_group_section",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_line_service_section",
                titleKey = "morphe_settings_catalog_hide_more_tab_line_service_section",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val bodySectionsMethod = AddMoreTabBodySectionsFingerprint.method
        val serviceSectionsMethod = AddMoreTabServiceSectionsFingerprint.method

        fun Fingerprint.hideViewHolder(settingMethod: String, labelPrefix: String) {
            matchAll(1 .. 1).single().method.hideViewHolder(listOf(settingMethod), labelPrefix)
        }

        fun Fingerprint.hideViewHolder(settingMethods: List<String>, labelPrefix: String) {
            matchAll(1 .. 1).single().method.hideViewHolder(settingMethods, labelPrefix)
        }

        bodySectionsMethod.hideKakaoPaySection(MoreTabKakaoPaySectionFingerprint.classDef.type)
        bodySectionsMethod.hideItemAdditions(
            SectionSpec(
                MoreTabGamePlaySectionFingerprint.classDef.type,
                "hideMoreTabGamePlaySection",
                "more_tab_game_play",
            ),
            SectionSpec(
                MoreTabKakaoNowSectionFingerprint.classDef.type,
                "hideMoreTabKakaoNowSection",
                "more_tab_kakao_now",
            ),
            SectionSpec(
                MoreTabWeatherSectionFingerprint.classDef.type,
                "hideMoreTabWeatherSection",
                "more_tab_weather",
            ),
            SectionSpec(
                MoreTabLineServiceSectionFingerprint.classDef.type,
                listOf("hideMoreTabLineServiceSection", "hideMoreTabServiceGroupSection"),
                "more_tab_line_service_body",
            ),
        )
        WeatherViewHolderBindFingerprint.method.hideWeatherViewHolder()

        serviceSectionsMethod.hideItemAdditions(
            SectionSpec(
                MoreTabServiceGroupSectionFingerprint.classDef.type,
                "hideMoreTabServiceGroupSection",
                "more_tab_service_group",
            ),
            SectionSpec(
                MoreTabLineServiceSectionFingerprint.classDef.type,
                listOf("hideMoreTabLineServiceSection", "hideMoreTabServiceGroupSection"),
                "more_tab_line_service",
            ),
        )

        val globalServiceGroupType = MoreTabGlobalServiceGroupSectionFingerprint.classDef.type
        moreTabGlobalServiceGroupAdditionFingerprint(globalServiceGroupType)
            .matchAll(1 .. 1)
            .single()
            .method
            .hideItemAdditions(
                SectionSpec(
                    globalServiceGroupType,
                    "hideMoreTabServiceGroupSection",
                    "more_tab_global_service_group",
                ),
            )

        moreTabServiceGroupViewHolderBindFingerprint(
            MoreTabServiceGroupSectionFingerprint.classDef.type,
        ).hideViewHolder(
            "hideMoreTabServiceGroupSection",
            "more_tab_service_group_view_holder",
        )
        moreTabGlobalServiceGroupViewHolderBindFingerprint(
            globalServiceGroupType,
        ).hideViewHolder(
            "hideMoreTabServiceGroupSection",
            "more_tab_global_service_group_view_holder",
        )
        moreTabLineServiceViewHolderBindFingerprint(
            MoreTabLineServiceSectionFingerprint.classDef.type,
        ).hideViewHolder(
            listOf("hideMoreTabLineServiceSection", "hideMoreTabServiceGroupSection"),
            "more_tab_line_service_view_holder",
        )
    }
}

private fun MutableMethod.hideWeatherViewHolder() {
    addInstructionsWithLabels(
        0,
        """
            invoke-static {}, $SETTINGS_CLASS->hideMoreTabWeatherSection()Z
            move-result v0
            iget-object v1, p0, Landroidx/recyclerview/widget/RecyclerView${'$'}F;->itemView:Landroid/view/View;
            if-eqz v0, :show_more_tab_weather_view_holder
            const/16 v0, 0x8
            invoke-virtual {v1, v0}, Landroid/view/View;->setVisibility(I)V
            return-void
            :show_more_tab_weather_view_holder
            const/4 v0, 0x0
            invoke-virtual {v1, v0}, Landroid/view/View;->setVisibility(I)V
        """.trimIndent(),
    )
}

private fun MutableMethod.hideViewHolder(settingMethods: List<String>, labelPrefix: String) {
    val hideLabel = "${labelPrefix}_hide"
    val showLabel = "${labelPrefix}_show"

    addInstructionsWithLabels(
        0,
        """
            ${settingMethods.hideConditionInstructions(0, hideLabel)}
            goto :$showLabel
            :$hideLabel
            iget-object p1, p0, Landroidx/recyclerview/widget/RecyclerView${'$'}F;->itemView:Landroid/view/View;
            const/16 v0, 0x8
            invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
            return-void
        """.trimIndent(),
        ExternalLabel(showLabel, getInstruction(0)),
    )
}

private fun MutableMethod.hideKakaoPaySection(itemType: String) {
    val itemIndex = instructions.indexOfFirst {
        it.opcode == Opcode.NEW_INSTANCE &&
                it.getReference<TypeReference>()?.type == itemType
    }.takeIf { it >= 0 }
        ?: throw PatchException("Could not find More tab KakaoPay item")
    val conditionIndex = (0 until itemIndex).firstOrNull {
        getInstruction(it).opcode == Opcode.IF_NEZ
    } ?: throw PatchException("Could not find More tab KakaoPay condition")
    val scratchRegister = (getInstruction(itemIndex) as? OneRegisterInstruction)?.registerA
        ?: throw PatchException("Could not find More tab KakaoPay skip register")

    addInstructionsWithLabels(
        conditionIndex,
        """
            invoke-static {}, $SETTINGS_CLASS->hideMoreTabKakaoPaySection()Z
            move-result v$scratchRegister
            if-nez v$scratchRegister, :hide_more_tab_kakao_pay_skip
        """.trimIndent(),
        ExternalLabel("hide_more_tab_kakao_pay_skip", branchTargetInstruction(conditionIndex)),
    )
}

private fun MutableMethod.hideItemAdditions(vararg specs: SectionSpec) {
    val specsByType = specs.associateBy { it.itemType }
    val insertions = buildList {
        var index = 0
        while (index < instructions.size) {
            val instruction = getInstruction(index)
            if (instruction.opcode != Opcode.NEW_INSTANCE) {
                index++
                continue
            }
            val spec = specsByType[instruction.getReference<TypeReference>()?.type]
            if (spec == null) {
                index++
                continue
            }

            val addIndex = (index + 1 until instructions.size).firstOrNull { candidateIndex ->
                val candidate = getInstruction(candidateIndex)
                val reference = candidate.getReference<MethodReference>()

                candidate.opcode == Opcode.INVOKE_INTERFACE &&
                        reference?.definingClass == "Ljava/util/List;" &&
                        reference.name == "add" &&
                        reference.returnType == "Z"
            } ?: throw PatchException("Could not find More tab section add call for ${spec.itemType}")
            val nextInstruction = getInstruction(addIndex + 1)
            val scratchRegister = (instruction as? OneRegisterInstruction)?.registerA
                ?: throw PatchException("Could not find More tab section scratch register for ${spec.itemType}")

            add(SectionSkip(instruction, nextInstruction, scratchRegister, spec, size))
            index = addIndex + 1
        }
    }

    val missingSpec = specs.firstOrNull { spec ->
        insertions.none { it.spec.itemType == spec.itemType }
    }
    if (missingSpec != null) {
        throw PatchException("Could not find More tab section additions for ${missingSpec.itemType}")
    }

    val guardedStarts = insertions.map { it.startInstruction }.toSet()
    val targetMarkers = insertions
        .asSequence()
        .mapNotNull { insertion -> insertion.nextInstruction.takeIf { it in guardedStarts } }
        .distinct()
        .sortedByDescending { instructions.indexOf(it) }
        .associateWith { instruction ->
            val index = instructions.indexOf(instruction)
            if (index < 0) {
                throw PatchException("Could not find More tab section marker target")
            }
            addInstructions(index, "nop")
            getInstruction(index)
        }

    insertions.asReversed().forEach { insertion ->
        val targetInstruction = targetMarkers[insertion.nextInstruction] ?: insertion.nextInstruction
        val label = "${insertion.spec.labelPrefix}_skip_${insertion.ordinal}"
        val index = instructions.indexOf(insertion.startInstruction)
        if (index < 0) {
            throw PatchException("Could not find More tab section insertion point for ${insertion.spec.itemType}")
        }

        addInstructionsWithLabels(
            index,
            insertion.spec.hideConditionInstructions(insertion.scratchRegister, label),
            ExternalLabel(label, targetInstruction),
        )
    }
}

private data class SectionSkip(
    val startInstruction: Instruction,
    val nextInstruction: Instruction,
    val scratchRegister: Int,
    val spec: SectionSpec,
    val ordinal: Int,
)

private data class SectionSpec(
    val itemType: String,
    val settingMethods: List<String>,
    val labelPrefix: String,
) {
    constructor(
        itemType: String,
        settingMethod: String,
        labelPrefix: String,
    ) : this(itemType, listOf(settingMethod), labelPrefix)

    fun hideConditionInstructions(register: Int, label: String): String =
        settingMethods.hideConditionInstructions(register, label)
}

private fun List<String>.hideConditionInstructions(register: Int, label: String): String =
    joinToString("\n") { method ->
        """
            invoke-static {}, $SETTINGS_CLASS->$method()Z
            move-result v$register
            if-nez v$register, :$label
        """.trimIndent()
    }

private fun MutableMethod.branchTargetInstruction(index: Int): Instruction {
    val offsets = instructions.instructionOffsets()
    val instruction = getInstruction(index) as? OffsetInstruction
        ?: throw PatchException("Could not read More tab branch target")
    val targetOffset = offsets[index] + instruction.codeOffset
    val targetIndex = offsets.indexOfFirst { it == targetOffset }
    if (targetIndex < 0) {
        throw PatchException("Could not resolve More tab branch target")
    }

    return getInstruction(targetIndex)
}

private fun List<Instruction>.instructionOffsets(): IntArray {
    var offset = 0
    return IntArray(size) { index ->
        offset.also {
            offset += this[index].codeUnits
        }
    }
}
