package app.revanced.patches.dcinside.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE

@Suppress("unused")
val addSettingsPatch = patch(
    name = "Add settings",
    description = "Adds a Morphe settings entry to the DCInside settings screen.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(registerSettingsActivityPatch, userMemoPatch)

    execute {
        SettingsFragmentOnViewCreatedFingerprint.method.addInstruction(
            0,
            "invoke-static {p1}, Lapp/revanced/extension/dcinside/settings/SettingsActivity;->bindSettingsShortcut(Landroid/view/View;)V",
        )
    }
}
