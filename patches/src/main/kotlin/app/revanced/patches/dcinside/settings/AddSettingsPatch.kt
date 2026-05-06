package app.revanced.patches.dcinside.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.util.getReference
import app.revanced.patches.dcinside.misc.addExtensionPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val addSettingsPatch = bytecodePatch(
    name = "Add settings",
    description = "Adds a Morphe settings entry to the DCInside settings screen.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(
        addExtensionPatch,
        addResourcesPatch,
        addSettingsResourcesPatch,
        registerSettingsActivityPatch,
        userMemoPatch,
    )

    execute {
        SettingsFragmentOnViewCreatedFingerprint.method.addInstructionsAfterOnViewCreatedSuperCall(
            superClass = "Landroidx/fragment/app/Fragment;",
            ownerName = "SettingsFragment",
            smaliInstructions = """
                invoke-static {p1}, Lapp/revanced/extension/dcinside/settings/SettingsActivity;->bindSettingsShortcut(Landroid/view/View;)V
            """.trimIndent(),
        )
    }
}

private fun MutableMethod.addInstructionsAfterOnViewCreatedSuperCall(
    superClass: String,
    ownerName: String,
    smaliInstructions: String,
) {
    val onViewCreatedSuperCallIndex = instructions.indexOfFirst {
        if (it.opcode != Opcode.INVOKE_SUPER) return@indexOfFirst false

        val reference = it.getReference<MethodReference>() ?: return@indexOfFirst false
        reference.definingClass == superClass &&
            reference.name == "onViewCreated" &&
            reference.returnType == "V"
    }

    check(onViewCreatedSuperCallIndex >= 0) {
        "Could not find $ownerName.onViewCreated super call in $definingClass->$name"
    }

    addInstructions(onViewCreatedSuperCallIndex + 1, smaliInstructions)
}
