package app.revanced.patches.dcinside.misc

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.misc.fingerprints.disableUpdateCheckFingerprint

@Suppress("unused")
val disableUpdateCheckPatch = bytecodePatch(
    name = "Disable update check",
    description = "Disables the app's update check.",
) {
    compatibleWith("com.dcinside.app.android"("5.2.5"))

    execute {
        disableUpdateCheckFingerprint.method.addInstruction(
            0,
            """
                const/4 v1, 0x0
                return v1
            """.trimIndent()
        )
    }
}