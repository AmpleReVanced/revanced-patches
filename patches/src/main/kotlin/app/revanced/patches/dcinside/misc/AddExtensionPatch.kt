package app.revanced.patches.dcinside.misc

import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val addExtensionPatch = bytecodePatch {
    extendWith("extensions/dcinside.mpe")
    dependsOn(sharedExtensionPatch)

    execute { /* NOP */ }
}