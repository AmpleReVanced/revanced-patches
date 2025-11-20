package app.revanced.patches.dcinside.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.ads.fingerprints.disableAdControllerFingerprint

@Suppress("unused")
val disableAdControllerPatch = bytecodePatch(
    name = "Disable ad controller",
    description = "Disables the ad controller that manages ads in the app.",
) {
    compatibleWith("com.dcinside.app.android"("5.2.1"))

    execute {
        disableAdControllerFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )
    }
}