package app.revanced.patches.unicorn.misc

import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.unicorn.shared.Constants.COMPATIBILITY_UNICORN

@Suppress("unused")
val addExtensionPatch = bytecodePatch {
    compatibleWith(COMPATIBILITY_UNICORN)
    extendWith("extensions/unicorn.mpe")

    execute { /* NOP */ }
}
