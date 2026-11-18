package app.revanced.patches.kakaotalk.misc.extension

import app.morphe.patcher.patch.patch
import app.revanced.patches.all.misc.versioncode.changeVersionCodePatch

@Suppress("unused")
val addExtensionPatch = patch {
    extendWith("extensions/kakaotalk.mpe")
    dependsOn(sharedExtensionPatch)

    execute { /* NOP */ }
}