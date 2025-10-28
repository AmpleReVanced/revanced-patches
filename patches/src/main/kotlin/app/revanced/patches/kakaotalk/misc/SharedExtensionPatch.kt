package app.revanced.patches.kakaotalk.misc

import app.revanced.patches.shared.misc.extension.sharedExtensionPatch
import app.revanced.patches.twitch.misc.extension.initHook

val sharedExtensionPatch = sharedExtensionPatch("kakaotalk", initHook)