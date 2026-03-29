package app.revanced.patches.kakaotalk.misc

import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.versioncode.changeVersionCodePatch

@Suppress("unused")
val addExtensionPatch = bytecodePatch {
    extendWith("extensions/kakaotalk.mpe")
    dependsOn(sharedExtensionPatch, changeVersionCodePatch)

    execute { /* NOP */ }
}