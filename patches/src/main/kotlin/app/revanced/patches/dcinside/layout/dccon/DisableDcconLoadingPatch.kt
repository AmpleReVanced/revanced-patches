package app.revanced.patches.dcinside.layout.dccon

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.patch
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/DisableDcconLoadingPatch;"

@Suppress("unused")
val disableDcconLoadingPatch = patch(
    name = "Disable DCCon loading",
    description = "Adds settings to block DCCon image loading in posts and replies.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addSettingsPatch, postDcconPatch)

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            SwitchPreference(
                key = "morphe_pref_block_post_dccon_loading",
                titleKey = "morphe_settings_block_post_dccon_loading",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_block_reply_dccon_loading",
                titleKey = "morphe_settings_block_reply_dccon_loading",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val replyDcconViewFields = ReplyDcconBindFingerprint.classDef.fields
            .filterDcconViewFields()

        ReplyDcconBindFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, $SETTINGS_CLASS->blockReplyDcconLoading()Z
                move-result v0
                if-eqz v0, :morphe_reply_dccon_continue

                const/16 v0, 0x8
                ${replyDcconViewFields.toHideVisibilityInstructions()}
                return-void

                :morphe_reply_dccon_continue
                nop
            """.trimIndent(),
        )
    }
}
