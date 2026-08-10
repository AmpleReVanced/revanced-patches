package app.revanced.patches.kakaotalk.misc.extension

import app.morphe.patcher.Fingerprint
import app.morphe.patches.all.misc.extension.ExtensionHook
import app.morphe.patches.all.misc.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(
    listOf("kakaotalk"),
    ExtensionHook(
        Fingerprint(
            definingClass = "Lcom/kakao/talk/application/App;",
            name = "onCreate",
            returnType = "V",
            parameters = emptyList(),
        )
    )
)