package app.revanced.patches.dcinside.misc

import app.morphe.patches.shared.misc.extension.activityOnCreateExtensionHook
import app.morphe.patches.shared.misc.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(
    "dcinside",
    activityOnCreateExtensionHook(
        "Lcom/dcinside/app/Application;",
        false
    )
)