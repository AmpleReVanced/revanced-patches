package app.revanced.patches.kakaomap.misc.extension

import app.morphe.patcher.Fingerprint
import app.morphe.patches.all.misc.extension.ExtensionHook
import app.morphe.patches.all.misc.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(
    listOf("kakaomap"),
    ExtensionHook(
        Fingerprint(
            definingClass = "Lcom/kakao/map/App;",
            name = "onCreate",
            returnType = "V",
            parameters = emptyList(),
        ),
    ),
)