package app.revanced.patches.dcinside.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE

@Suppress("unused")
val disableUpdateCheckPatch = bytecodePatch(
    name = "Disable update check",
    description = "Disables the app's update check.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

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