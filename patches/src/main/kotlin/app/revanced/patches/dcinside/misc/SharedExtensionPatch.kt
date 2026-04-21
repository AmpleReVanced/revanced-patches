package app.revanced.patches.dcinside.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patches.all.misc.extension.ExtensionHook
import app.morphe.patches.all.misc.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(
    listOf("dcinside"),
    ExtensionHook(
        Fingerprint(
            definingClass = "Lcom/dcinside/app/Application;",
            name = "onCreate",
            returnType = "V",
            parameters = emptyList(),
        )
    )
)