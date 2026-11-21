package app.revanced.patches.dcinside.misc.extension

import app.morphe.patcher.patch.patch

@Suppress("unused")
val addExtensionPatch = patch {
    extendWith("extensions/dcinside.mpe")
    dependsOn(sharedExtensionPatch)

    execute { /* NOP */ }
}