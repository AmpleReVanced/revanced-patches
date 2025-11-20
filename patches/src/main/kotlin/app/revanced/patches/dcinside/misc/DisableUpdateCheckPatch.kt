package app.revanced.patches.dcinside.misc

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.misc.fingerprints.disableUpdateCheckFingerprint

@Suppress("unused")
val disableUpdateCheckPatch = bytecodePatch(
    name = "Disable update check",
    description = "Disables the app's update check.",
) {
    compatibleWith("com.dcinside.app.android"("5.2.1"))

    execute {
        disableUpdateCheckFingerprint.method.addInstruction(
            0,
            """
                return-void
            """.trimIndent()
        )
    }
}