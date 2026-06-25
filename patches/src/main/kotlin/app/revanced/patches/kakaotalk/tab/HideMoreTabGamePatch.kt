package app.revanced.patches.kakaotalk.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tab.fingerprints.MoreTabPagerGameItemFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.MoreTabPagerHomeItemFingerprint
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/HideMoreTabGamePatch;"

@Suppress("unused")
val hideMoreTabGamePatch = bytecodePatch(
    name = "Hide More tab Game tab",
    description = "Hides the Game tab from the More tab.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.NAVIGATION.addPreferences(
            SwitchPreference(
                key = "morphe_pref_hide_more_tab_game",
                titleKey = "morphe_settings_catalog_hide_more_tab_game",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val gameItemClass = MoreTabPagerGameItemFingerprint.classDef
        val homeItemClass = MoreTabPagerHomeItemFingerprint.classDef
        val pagerItemType = gameItemClass.superclass
            ?: throw PatchException("Could not infer MoreTabPagerItem super type")

        val itemsFlowMethods = mutableListOf<Pair<ClassDef, Method>>()
        classDefForEach { classDef ->
            classDef.methods.filter { method ->
                val instructions = method.implementation?.instructions ?: return@filter false

                instructions.any {
                    it.opcode == Opcode.SGET_OBJECT &&
                            it.getReference<FieldReference>()?.type == gameItemClass.type
                } &&
                        instructions.any {
                            it.opcode == Opcode.NEW_ARRAY &&
                                    it.getReference<TypeReference>()?.type == "[$pagerItemType"
                        } &&
                        instructions.any {
                            val reference = it.getReference<MethodReference>()

                            it.opcode == Opcode.INVOKE_STATIC &&
                                    reference?.name == "listOfNotNull" &&
                                    reference.returnType == "Ljava/util/List;"
                        }
            }.forEach { method ->
                itemsFlowMethods += classDef to method
            }
        }
        val (itemsFlowClassDef, itemsFlowMethodDef) = itemsFlowMethods.singleOrNull()
            ?: throw PatchException("Could not find More tab pager items flow method")
        val itemsFlowMethod = mutableClassDefBy(itemsFlowClassDef).findMutableMethodOf(itemsFlowMethodDef)

        val gameFieldIndex = itemsFlowMethod.indexOfFirstInstructionOrThrow {
            opcode == Opcode.SGET_OBJECT &&
                    getReference<FieldReference>()?.type == gameItemClass.type
        }
        val gameFieldRegister = (itemsFlowMethod.getInstruction(gameFieldIndex) as? OneRegisterInstruction)
            ?.registerA
            ?: throw PatchException("Could not find Game tab singleton register")

        val listOfNotNullIndex = itemsFlowMethod.indexOfFirstInstructionOrThrow(gameFieldIndex) {
            val reference = getReference<MethodReference>()

            opcode == Opcode.INVOKE_STATIC &&
                    reference?.definingClass == "Lkotlin/collections/CollectionsKt;" &&
                    reference.name == "listOfNotNull" &&
                    reference.parameterTypes == listOf("[Ljava/lang/Object;") &&
                    reference.returnType == "Ljava/util/List;"
        }
        val newPagerItemArrayIndex = (gameFieldIndex until listOfNotNullIndex).firstOrNull { index ->
            val instruction = itemsFlowMethod.getInstruction(index)

            instruction.opcode == Opcode.NEW_ARRAY &&
                    instruction.getReference<TypeReference>()?.type == "[$pagerItemType"
        } ?: throw PatchException("Could not find MoreTabPagerItem array creation")
        val gameItemRegister = (gameFieldIndex + 1 until newPagerItemArrayIndex).firstNotNullOfOrNull { index ->
            val instruction = itemsFlowMethod.getInstruction(index)

            if (instruction.opcode == Opcode.MOVE_OBJECT &&
                instruction is TwoRegisterInstruction &&
                instruction.registerB == gameFieldRegister
            ) {
                instruction.registerA
            } else {
                null
            }
        } ?: throw PatchException("Could not find Game tab item register")

        itemsFlowMethod.addInstructionsWithLabels(
            newPagerItemArrayIndex,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->hideMoreTabGame()Z
                move-result v$gameFieldRegister
                if-eqz v$gameFieldRegister, :morphe_keep_more_tab_game
                const/4 v$gameItemRegister, 0x0
                :morphe_keep_more_tab_game
                nop
            """.trimIndent(),
        )

        val schemeSelectionMethods = mutableListOf<Pair<ClassDef, Method>>()
        classDefForEach { classDef ->
            classDef.methods.filter { method ->
                method.parameterTypes == listOf("Ljava/lang/String;", "Landroid/net/Uri;") &&
                        method.returnType == "V" &&
                        method.implementation?.instructions?.let { instructions ->
                            instructions.any {
                                it.opcode == Opcode.SGET_OBJECT &&
                                        it.getReference<FieldReference>()?.type == gameItemClass.type
                            } &&
                                    instructions.any {
                                        it.opcode == Opcode.SGET_OBJECT &&
                                                it.getReference<FieldReference>()?.type == homeItemClass.type
                                    } &&
                                    instructions.any {
                                        val reference = it.getReference<MethodReference>()

                                        it.opcode == Opcode.INVOKE_STATIC &&
                                                reference?.definingClass == "Lkotlin/jvm/internal/Intrinsics;" &&
                                                reference.name == "e" &&
                                                reference.returnType == "Z"
                                    }
                        } == true
            }.forEach { method ->
                schemeSelectionMethods += classDef to method
            }
        }
        val (schemeSelectionClassDef, schemeSelectionMethodDef) = schemeSelectionMethods.singleOrNull()
            ?: throw PatchException("Could not find More tab pager scheme selection method")
        val schemeSelectionMethod = mutableClassDefBy(schemeSelectionClassDef).findMutableMethodOf(schemeSelectionMethodDef)

        val gameSelectionIndex = schemeSelectionMethod.indexOfFirstInstructionOrThrow {
            opcode == Opcode.SGET_OBJECT &&
                    getReference<FieldReference>()?.type == gameItemClass.type
        }
        val selectedTabRegister = (schemeSelectionMethod.getInstruction(gameSelectionIndex) as? BuilderInstruction21c)
            ?.registerA
            ?: throw PatchException("Could not find selected More tab item register")
        val gameSelectionField = schemeSelectionMethod.getInstruction(gameSelectionIndex)
            .getReference<FieldReference>()
            ?: throw PatchException("Could not find Game tab singleton field")
        val gameComparisonIndex = schemeSelectionMethod.indexOfFirstInstructionOrThrow(gameSelectionIndex) {
            val reference = getReference<MethodReference>()

            opcode == Opcode.INVOKE_STATIC &&
                    reference?.definingClass == "Lkotlin/jvm/internal/Intrinsics;" &&
                    reference.name == "e" &&
                    reference.parameterTypes == listOf("Ljava/lang/Object;", "Ljava/lang/Object;") &&
                    reference.returnType == "Z"
        }
        val scratchRegister = (schemeSelectionMethod.getInstruction(gameComparisonIndex) as? FiveRegisterInstruction)
            ?.registerD
            ?: throw PatchException("Could not find Game tab comparison scratch register")
        val homeSelectionField = schemeSelectionMethod.instructions.firstOrNull {
            it.opcode == Opcode.SGET_OBJECT &&
                    it.getReference<FieldReference>()?.type == homeItemClass.type
        }?.getReference<FieldReference>()
            ?: throw PatchException("Could not find Home tab singleton field")
        val launchSelectionIndex = schemeSelectionMethod.indexOfFirstInstructionOrThrow(gameSelectionIndex) {
            opcode == Opcode.NEW_INSTANCE
        }

        schemeSelectionMethod.addInstructionsWithLabels(
            launchSelectionIndex,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->hideMoreTabGame()Z
                move-result v$scratchRegister
                if-eqz v$scratchRegister, :morphe_keep_more_tab_scheme_selection
                sget-object v$scratchRegister, ${gameSelectionField.smaliReference}
                invoke-static {v$selectedTabRegister, v$scratchRegister}, Lkotlin/jvm/internal/Intrinsics;->e(Ljava/lang/Object;Ljava/lang/Object;)Z
                move-result v$scratchRegister
                if-eqz v$scratchRegister, :morphe_keep_more_tab_scheme_selection
                sget-object v$selectedTabRegister, ${homeSelectionField.smaliReference}
                :morphe_keep_more_tab_scheme_selection
                nop
            """.trimIndent(),
        )
    }
}
