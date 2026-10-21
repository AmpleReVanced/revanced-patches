package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.NowBrandChipFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.NowChildTabFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.NowChildTabFromNameFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.NowChildTabFromPositionFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.NowTabChipStateFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.nowChildTabSelectionFingerprint
import app.revanced.patches.kakaotalk.layout.tab.fingerprints.nowTabChipFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/RemoveShortFormTabPatch;"
private const val SETTINGS_CLASS = "Lapp/revanced/extension/kakaotalk/settings/Settings;"
private const val NORMALIZE_TAB_METHOD = "revanced_normalizeNowChildTab"

@Suppress("unused")
val removeShortFormTabPatch = bytecodePatch(
    name = "Remove Short-form Tab",
    description = "Removes the Short-form tab from the now screen.",
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

        val tabClass = NowChildTabFingerprint.classDef
        val tabType = tabClass.type
        fun tabField(name: String) = tabClass.fields.single { it.name == name }.smaliReference
        val openLinkField = tabField("Openlink")
        val normalizeTab = "$tabType->$NORMALIZE_TAB_METHOD($tabType)$tabType"

        tabClass.methods.add(
            ImmutableMethod(
                tabType,
                NORMALIZE_TAB_METHOD,
                listOf(ImmutableMethodParameter(tabType, null, null)),
                tabType,
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value or AccessFlags.FINAL.value,
                null,
                null,
                MutableMethodImplementation(2),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $SETTINGS_CLASS->removeShortFormTab()Z
                        move-result v0
                        if-eqz v0, :keep_tab
                        sget-object v0, ${tabField("ShortForm")}
                        if-eq p0, v0, :use_openlink
                        sget-object v0, ${tabField("Brand")}
                        if-ne p0, v0, :keep_tab
                        :use_openlink
                        sget-object p0, $openLinkField
                        :keep_tab
                        return-object p0
                    """,
                )
            },
        )

        nowChildTabSelectionFingerprint(tabType).matchAll().forEach { match ->
            val method = match.method
            var parameterRegister = if (AccessFlags.STATIC.isSet(method.accessFlags)) 0 else 1
            val instructions = buildString {
                method.parameterTypes.forEach { type ->
                    if (type == tabType) {
                        appendLine("invoke-static/range {p$parameterRegister .. p$parameterRegister}, $normalizeTab")
                        appendLine("move-result-object p$parameterRegister")
                    }
                    parameterRegister += if (type == "J" || type == "D") 2 else 1
                }
            }
            method.addInstructions(0, instructions)
        }

        listOf(NowChildTabFromPositionFingerprint, NowChildTabFromNameFingerprint).forEach { fingerprint ->
            fingerprint.method.apply {
                findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                    val register = getInstruction<OneRegisterInstruction>(index).registerA
                    replaceInstruction(index, "nop")
                    addInstructions(
                        index + 1,
                        """
                            invoke-static/range {v$register .. v$register}, $normalizeTab
                            move-result-object v$register
                            return-object v$register
                        """,
                    )
                }
            }
        }

        val chipStateClass = NowTabChipStateFingerprint.classDef
        val chipTabGetter = chipStateClass.methods.single {
            it.parameterTypes.isEmpty() && it.returnType == tabType
        }
        nowTabChipFingerprint(chipStateClass.type).method.apply {
            val registers = getFreeRegisterProvider(0, 2)
            val tabRegister = registers.getFreeRegister4Bit()
            val flagRegister = registers.getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $SETTINGS_CLASS->removeShortFormTab()Z
                    move-result v$flagRegister
                    if-eqz v$flagRegister, :keep_chip
                    invoke-virtual/range {p0 .. p0}, ${chipTabGetter.smaliReference}
                    move-result-object v$tabRegister
                    sget-object v$flagRegister, $openLinkField
                    if-eq v$tabRegister, v$flagRegister, :keep_chip
                    return-void
                    :keep_chip
                    nop
                """,
            )
        }
        NowBrandChipFingerprint.method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $SETTINGS_CLASS->removeShortFormTab()Z
                    move-result v$register
                    if-eqz v$register, :keep_brand_chip
                    return-void
                    :keep_brand_chip
                    nop
                """,
            )
        }
    }
}
