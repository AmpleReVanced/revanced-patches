package app.revanced.patches.kakaotalk.misc

import app.morphe.patches.shared.misc.extension.activityOnCreateExtensionHook
import app.morphe.patches.shared.misc.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(
    "kakaotalk",
    activityOnCreateExtensionHook(
        "Lcom/kakao/talk/application/App;",
        false
    )
)