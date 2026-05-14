package app.revanced.patches.dcinside.dccon

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE

@Suppress("unused")
val renderBigDcconAsNormalPatch = bytecodePatch(
    name = "Render big DCCon as normal size",
    description = "Adds a setting to render big DCCon at normal DCCon size.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addSettingsPatch)

    execute {
        val postElementMethods = PostDcconImageHandlerFingerprint.method.inferPostElementMethods(this)

        PostDcconImageHandlerFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, $SETTINGS_CLASS->renderBigDcconAsNormal()Z
                move-result v0
                if-eqz v0, :morphe_post_big_dccon_continue

                move-object/from16 v1, p1
                const-string v0, "bigdccon"
                ${postElementMethods.removeClassInvoke} {v1, v0}, ${postElementMethods.removeClass}

                :morphe_post_big_dccon_continue
                nop
            """.trimIndent(),
        )

        val replyDcconImageFields = ReplyDcconBindFingerprint.method.inferReplyDcconImageFields(
            ReplyDcconBindFingerprint.classDef.methods,
        )

        ReplyDcconBindFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, $SETTINGS_CLASS->renderBigDcconAsNormal()Z
                move-result v0
                if-eqz v0, :morphe_reply_big_dccon_continue

                const/4 p2, 0x0
                ${replyDcconImageFields.toResizeInstructions()}

                :morphe_reply_big_dccon_continue
                nop
            """.trimIndent(),
        )

        ReplyDcconAdapterBindFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, $SETTINGS_CLASS->renderBigDcconAsNormal()Z
                move-result v0
                if-eqz v0, :morphe_reply_adapter_big_dccon_continue

                sget-object p4, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;

                :morphe_reply_adapter_big_dccon_continue
                nop
            """.trimIndent(),
        )
    }
}
