package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.Fingerprint

internal object CatchAdListFingerprint : Fingerprint(
    name = "isAd",
    parameters = listOf(),
    returnType = "Ljava/util/List;",
    custom = { _, classDef ->
        classDef.type == "Lcom/afreecatv/domain/catchapi/model/CatchResult;"
    },
)