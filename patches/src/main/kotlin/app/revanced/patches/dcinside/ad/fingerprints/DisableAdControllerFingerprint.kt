package app.revanced.patches.dcinside.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object AdPlacementUnitsFingerprint : Fingerprint(
    filters = listOf(
        string("and_ad_banner_read_top"),
        methodCall(
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Ljava/util/List;",
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object InitializeAdSdksFingerprint : Fingerprint(
    isStatic = true,
    parameters = listOf("L"),
    returnType = "V",
    filters = listOf(
        methodCall("Lcom/gomfactory/adpie/sdk/AdPieSDK;->initialize(Landroid/content/Context;Ljava/lang/String;)V"),
    ),
)
