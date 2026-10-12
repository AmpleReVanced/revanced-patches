package app.revanced.patches.starnote.login

import app.morphe.patcher.patch.rawResourcePatch
import app.revanced.patches.starnote.shared.Constants.COMPATIBILITY_STARNOTE
import app.revanced.patches.starnote.shared.ProtectedDex
import app.revanced.patches.starnote.shared.loadProtectedDexPatch

@Suppress("unused")
val bypassLoginPatch = rawResourcePatch(
    name = "Bypass login",
    description = "Skips the mandatory account sign-in on launch and opens the app directly.",
) {
    compatibleWith(COMPATIBILITY_STARNOTE)

    dependsOn(loadProtectedDexPatch)

    execute {
        ProtectedDex.returnEarly(LoginRequiredFingerprint, false)
    }
}