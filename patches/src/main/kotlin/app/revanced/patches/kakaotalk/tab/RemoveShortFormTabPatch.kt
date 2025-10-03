package app.revanced.patches.kakaotalk.tab

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.tab.fingerprints.chooseTabShortFormOrOpenLinkFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.fragmentMainTabNowBindingFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.getOpenLinkModuleFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.nowFragmentOnViewCreatedFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.nowTabPagerAdapterFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.transitionOpenLinkOrShortformMethodFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.viewPager2VerifyFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21s
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

@Suppress("unused")
val removeShortFormTabPatch = bytecodePatch(
    name = "Remove Short-form Tab",
    description = "Removes the Short-form tab from the now fragment.",
) {
    compatibleWith("com.kakao.talk"("25.8.3"))

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

        val fragmentMainTabNowBindingClass = fragmentMainTabNowBindingFingerprint.classDef
        val viewPager2Field = fragmentMainTabNowBindingClass.fields.first {
            it.type == "Landroidx/viewpager2/widget/ViewPager2;"
        }
        val viewPager2VerifyMethod = viewPager2VerifyFingerprint.method

        val chooseTabShortFormOrOpenLinkMethod = chooseTabShortFormOrOpenLinkFingerprint.method

        val sgetObject = chooseTabShortFormOrOpenLinkMethod.instructions.first {
            it.opcode == Opcode.SGET_OBJECT
        }.getReference<FieldReference>()!!
        val invokeStatic = chooseTabShortFormOrOpenLinkMethod.getInstruction(0) as BuilderInstruction35c

        val invokeVirtual = chooseTabShortFormOrOpenLinkMethod.instructions.filter {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                    it.getReference<MethodReference>()?.definingClass == sgetObject.definingClass
        }.getOrNull(1)
        val lastInvokeVirtual = chooseTabShortFormOrOpenLinkMethod.getInstruction(
            chooseTabShortFormOrOpenLinkMethod.instructions.indexOfLast {
                it.opcode == Opcode.INVOKE_VIRTUAL
            }
        ) as BuilderInstruction35c

        chooseTabShortFormOrOpenLinkMethod.addInstructions(
            0,
            """
                iget-object v0, p0, ${chooseTabShortFormOrOpenLinkMethod.definingClass}->I:${viewPager2Field.definingClass}
                if-nez v0, :not_null
                const-string v1, "binding"
                invoke-static {v1}, Lkotlin/jvm/internal/w;->B(Ljava/lang/String;)V
                return-void
                
                :not_null
                iget-object v1, v0, ${viewPager2Field.definingClass}->${viewPager2Field.name}:Landroidx/viewpager2/widget/ViewPager2;
                const/4 v2, 0x0
                invoke-virtual {v1, v2, v2}, Landroidx/viewpager2/widget/ViewPager2;->${viewPager2VerifyMethod.name}(IZ)V
                
                sget-object v1, ${sgetObject.definingClass}->${sgetObject.name}:${sgetObject.definingClass}
                sget-object v2, ${invokeStatic.getReference<MethodReference>()?.definingClass}->Openlink:${invokeStatic.getReference<MethodReference>()?.definingClass}
                invoke-virtual {v1, v2}, ${invokeVirtual?.getReference<MethodReference>().toString()}
                
                invoke-virtual {p0, v2}, ${lastInvokeVirtual.getReference<MethodReference>().toString()}
                return-void
            """.trimIndent()
        )
    }

}