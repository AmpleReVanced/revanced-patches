package app.revanced.patches.kakaotalk.misc.extension

import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.versioncode.changeVersionCodePatch

@Suppress("unused")
val addExtensionPatch = bytecodePatch {
    extendWith("extensions/kakaotalk.mpe")
    dependsOn(sharedExtensionPatch)

    execute { /* NOP */ }
}