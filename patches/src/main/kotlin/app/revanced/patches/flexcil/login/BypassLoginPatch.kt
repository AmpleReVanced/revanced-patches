package app.revanced.patches.flexcil.login

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.flexcil.shared.Constants.COMPATIBILITY_FLEXCIL

@Suppress("unused")
val bypassLoginPatch = bytecodePatch(
    name = "Bypass login",
    description = "Skips the mandatory account sign-in on launch and opens the app directly.",
) {
    compatibleWith(COMPATIBILITY_FLEXCIL)

    execute {
        // The launcher activity shows the sign-in UI whenever the user is not
        // logged in. Redirect that path straight to MainActivity instead.
        ShowAccountLoginFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Landroid/content/Intent;
                const-class v1, Lcom/flexcil/flexcilnote/MainActivity;
                invoke-direct { v0, p0, v1 }, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                invoke-virtual { p0, v0 }, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
                invoke-virtual { p0 }, Landroid/app/Activity;->finish()V
                return-void
            """,
        )
    }
}