package app.revanced.patches.dcinside.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.ads.fingerprints.disableAdControllerFingerprint
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE

@Suppress("unused")
val disableAdControllerPatch = bytecodePatch(
    name = "Disable ad controller",
    description = "Disables the ad controller that manages ads in the app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        disableAdControllerFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )
    }
}