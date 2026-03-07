package app.revanced.patches.kakaotalk.tab

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.tab.fingerprints.chooseNowChildTabFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.getOpenLinkModuleFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.nowFragmentOnViewCreatedFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.nowTabPagerAdapterFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.transitionOpenLinkOrShortformMethodFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21s
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

@Suppress("unused")
val removeShortFormTabPatch = bytecodePatch(
    name = "Remove Short-form Tab",
    description = "Removes the Short-form tab from the now fragment.",
) {
    compatibleWith("com.kakao.talk"("26.2.0"))

    execute {
        val onViewCreated = nowFragmentOnViewCreatedFingerprint.method

        val igetObject = onViewCreated.instructions.first {
            it.opcode == Opcode.IGET_OBJECT &&
                    it.getReference<FieldReference>()?.type == "Lcom/kakao/talk/core/ui/widget/TdChip;"
        } as BuilderInstruction22c

        val viewReg = igetObject.registerA
        val intReg = 3
        val insertIndex = onViewCreated.instructions.indexOf(igetObject) + 1

        onViewCreated.addInstructions(
            insertIndex,
            listOf(
                BuilderInstruction21s(
                    Opcode.CONST_16,
                    intReg,
                    0x8
                ),
                BuilderInstruction35c(
                    Opcode.INVOKE_VIRTUAL,
                    2,
                    viewReg,
                    intReg,
                    0, 0, 0,
                    ImmutableMethodReference(
                        "Landroid/view/View;",
                        "setVisibility",
                        listOf(
                            "I"
                        ),
                        "V"
                    )
                )
            )
        )

        val getChildTab = onViewCreated.instructions.last { it.opcode == Opcode.CHECK_CAST } as BuilderInstruction21c
        val fieldRef = getChildTab.getReference<TypeReference>()

        onViewCreated.addInstructions(
            onViewCreated.instructions.indexOfLast { it.opcode == Opcode.MOVE_RESULT_OBJECT } + 1,
            "sget-object v0, ${fieldRef!!.type}->Openlink:${fieldRef.type}"
        )

        val getItemCountMethod = nowTabPagerAdapterFingerprint.classDef.methods.first { it.name == "getItemCount" }
        getItemCountMethod.replaceInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )

        val getOpenLinkModuleMethod = getOpenLinkModuleFingerprint.method

        val createFragmentMethod = nowTabPagerAdapterFingerprint.method
        createFragmentMethod.replaceInstructions(
            0,
            """
                invoke-static {}, ${getOpenLinkModuleMethod.definingClass}->${getOpenLinkModuleMethod.name}()Lcom/kakao/talk/module/openlink/contract/OpenLinkModuleFacade;
                move-result-object v0

                invoke-interface {v0}, Lcom/kakao/talk/module/openlink/contract/OpenLinkModuleFacade;->createOpenChatTabFragment()Landroidx/fragment/app/Fragment;
                move-result-object v0

                return-object v0
            """.trimIndent()
        )

        val transitionOpenLinkOrShortformMethod = transitionOpenLinkOrShortformMethodFingerprint.method
        transitionOpenLinkOrShortformMethod.replaceInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )

        val chooseNowChildTabMethod = chooseNowChildTabFingerprint.method
        val getPositionIdx = chooseNowChildTabMethod.instructions.indexOfFirst {
            if (it.opcode != Opcode.INVOKE_VIRTUAL) return@indexOfFirst false

            val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
                ?: return@indexOfFirst false

            ref.definingClass == fieldRef.type &&
                    ref.name == "getPosition" &&
                    ref.returnType == "I" &&
                    ref.parameterTypes.isEmpty()
        }
        check(getPositionIdx >= 0) {
            "LSj/b;->getPosition()I invoke not found"
        }

        val tabRegister = when (val invokeInsn = chooseNowChildTabMethod.getInstruction(getPositionIdx)) {
            is BuilderInstruction35c -> invokeInsn.registerC
            is BuilderInstruction3rc -> invokeInsn.startRegister
            else -> error("Unsupported invoke instruction type: ${invokeInsn::class.java.name}")
        }

        chooseNowChildTabMethod.addInstruction(
            getPositionIdx,
            BuilderInstruction21c(
                Opcode.SGET_OBJECT,
                tabRegister,
                ImmutableFieldReference(
                    fieldRef.type,
                    "Openlink",
                    fieldRef.type
                )
            )
        )
    }

}