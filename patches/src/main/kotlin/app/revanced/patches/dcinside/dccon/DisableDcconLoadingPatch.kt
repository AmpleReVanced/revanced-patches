package app.revanced.patches.dcinside.dccon

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.settings.PreferenceScreen
import app.revanced.patches.dcinside.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/DisableDcconLoadingPatch;"

@Suppress("unused")
val disableDcconLoadingPatch = bytecodePatch(
    name = "Disable DCCon loading",
    description = "Adds settings to block DCCon image loading in posts and replies.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addSettingsPatch)

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

        val postElementMethods = PostDcconImageHandlerFingerprint.method.inferPostElementMethods(this)

        PostDcconImageHandlerFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, $SETTINGS_CLASS->blockPostDcconLoading()Z
                move-result v0
                if-eqz v0, :morphe_post_dccon_continue

                move-object/from16 v1, p1
                const-string v0, "src"
                ${postElementMethods.attrInvoke} {v1, v0}, ${postElementMethods.attr}
                move-result-object v0
                invoke-static {v0}, $SETTINGS_CLASS->isDcconUrl(Ljava/lang/String;)Z
                move-result v0
                if-nez v0, :morphe_post_dccon_block

                ${postElementMethods.datasetInvoke} {v1}, ${postElementMethods.dataset}
                move-result-object v0
                const-string v2, "src"
                invoke-interface {v0, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v2
                check-cast v2, Ljava/lang/String;
                invoke-static {v2}, $SETTINGS_CLASS->isDcconUrl(Ljava/lang/String;)Z
                move-result v2
                if-nez v2, :morphe_post_dccon_block

                const-string v2, "original"
                invoke-interface {v0, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v0
                check-cast v0, Ljava/lang/String;
                invoke-static {v0}, $SETTINGS_CLASS->isDcconUrl(Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :morphe_post_dccon_continue

                :morphe_post_dccon_block
                ${postElementMethods.removeInvoke} {v1}, ${postElementMethods.remove}
                return-void

                :morphe_post_dccon_continue
                nop
            """.trimIndent(),
        )

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
