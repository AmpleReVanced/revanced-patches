package app.revanced.patches.soop.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.soop.ads.fingerprints.*
import app.revanced.patches.soop.shared.Constants.COMPATIBILITY_SOOP
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val removeMainboardAdsPatch = bytecodePatch(
    name = "Remove main board ads",
    description = "Removes the native \"main board\" banner ad shown in feeds and under the live player.",
) {
    compatibleWith(COMPATIBILITY_SOOP)

    execute {
        val bind = MainBoardAdBindFingerprint.method
        val holderClass = mutableClassDefBy(bind.definingClass)

        val bindingField = bind.instructions
            .mapNotNull { (it as? ReferenceInstruction)?.reference as? FieldReference }
            .firstOrNull { it.definingClass == bind.definingClass }
            ?: throw PatchException("Could not find the holder binding field.")

        val rootViewField = holderClass.methods
            .filter { it.name == "<init>" }
            .firstNotNullOfOrNull { constructor ->
                constructor.instructions
                    .mapNotNull { (it as? ReferenceInstruction)?.reference as? FieldReference }
                    .firstOrNull { it.type == "Landroid/view/View;" }
            }
            ?: throw PatchException("Could not find the holder root view field.")

        bind.addInstructions(
            0,
            """
                move-object/from16 v0, p0
                iget-object v0, v0, ${bindingField.definingClass}->${bindingField.name}:${bindingField.type}
                iget-object v0, v0, ${rootViewField.definingClass}->${rootViewField.name}:${rootViewField.type}
                const/16 v1, 0x8
                invoke-virtual { v0, v1 }, Landroid/view/View;->setVisibility(I)V
                return-void
            """,
        )
    }
}