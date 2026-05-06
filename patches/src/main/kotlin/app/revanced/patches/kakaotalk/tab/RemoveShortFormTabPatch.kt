package app.revanced.patches.kakaotalk.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tab.fingerprints.ChooseNowChildTabFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.NowFragmentOnViewCreatedFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.NowTabPagerAdapterFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.TransitionOpenLinkOrShortformMethodFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

@Suppress("unused")
val removeShortFormTabPatch = bytecodePatch(
    name = "Remove Short-form Tab",
    description = "Removes the Short-form tab from the now fragment.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch)

    execute {
        val onViewCreated = NowFragmentOnViewCreatedFingerprint.method
        val shortFormChip = onViewCreated.instructions.firstOrNull {
            it.opcode == Opcode.IGET_OBJECT &&
                    it.getReference<FieldReference>()?.type == "Lcom/kakao/talk/core/ui/widget/TdChip;"
        } as? BuilderInstruction22c ?: throw PatchException("Could not find short-form chip field in NowFragment.onViewCreated")

        onViewCreated.addInstructionsWithLabels(
            onViewCreated.instructions.indexOf(shortFormChip) + 1,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result v3
                if-eqz v3, :morphe_keep_short_form_chip
                const/16 v3, 0x8
                invoke-virtual {p1, v3}, Landroid/view/View;->setVisibility(I)V
                :morphe_keep_short_form_chip
                nop
            """.trimIndent()
        )

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
        val shortFormField = createFragmentMethod.instructions.firstOrNull {
            it.opcode == Opcode.SGET_OBJECT &&
                    it.getReference<FieldReference>()?.name == "ShortForm"
        }?.getReference<FieldReference>()
            ?: throw PatchException("Could not find ShortForm field in NowTabPagerAdapter.w")
        val openLinkField = createFragmentMethod.instructions.firstOrNull {
            it.opcode == Opcode.SGET_OBJECT &&
                    it.getReference<FieldReference>()?.name == "Openlink"
        }?.getReference<FieldReference>()
            ?: throw PatchException("Could not find Openlink field in NowTabPagerAdapter.w")
        val getPositionMethod = createFragmentMethod.instructions.firstOrNull {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                    it.getReference<MethodReference>()?.name == "getPosition" &&
                    it.getReference<MethodReference>()?.returnType == "I"
        }?.getReference<MethodReference>()
            ?: throw PatchException("Could not find getPosition()I call in NowTabPagerAdapter.w")

        createFragmentMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result v0
                if-eqz v0, :morphe_original_fragment
                sget-object v0, $shortFormField
                invoke-virtual {v0}, $getPositionMethod
                move-result v0
                if-ne p1, v0, :morphe_original_fragment
                sget-object p1, $openLinkField
                invoke-virtual {p1}, $getPositionMethod
                move-result p1
                :morphe_original_fragment
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

        val chooseNowChildTabMethod = ChooseNowChildTabFingerprint.method
        val getPositionIdx = chooseNowChildTabMethod.instructions.indexOfFirst {
            if (it.opcode != Opcode.INVOKE_VIRTUAL) return@indexOfFirst false

            val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
                ?: return@indexOfFirst false

            ref.definingClass == fieldRef.type &&
                    ref.name == "getPosition" &&
                    ref.returnType == "I" &&
                    ref.parameterTypes.isEmpty()
        }
        if (getPositionIdx < 0) {
            throw PatchException("Could not find getPosition()I invoke in chooseNowChildTab")
        }

        val tabRegister = when (val invokeInsn = chooseNowChildTabMethod.getInstruction(getPositionIdx)) {
            is BuilderInstruction35c -> invokeInsn.registerC
            is BuilderInstruction3rc -> invokeInsn.startRegister
            else -> throw PatchException("Unsupported invoke instruction type in chooseNowChildTab: ${invokeInsn::class.java.name}")
        }

        chooseNowChildTabMethod.addInstructionsWithLabels(
            getPositionIdx,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->removeShortFormTab()Z
                move-result p1
                if-eqz p1, :morphe_keep_now_child_tab
                sget-object v$tabRegister, ${fieldRef.type}->Openlink:${fieldRef.type}
                :morphe_keep_now_child_tab
                nop
            """.trimIndent()
        )
    }
}
