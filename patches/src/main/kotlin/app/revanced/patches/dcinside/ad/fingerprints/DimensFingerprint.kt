package app.revanced.patches.dcinside.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral

internal object GetMinimumDimensFingerprint : Fingerprint(
    parameters = listOf("Ljava/util/List;"),
    returnType = "I",
    filters = listOf(
        resourceLiteral(ResourceType.DIMEN, "ad_main_small_native"),
        resourceLiteral(ResourceType.DIMEN, "ad_minimum"),
    ),
)

internal object ReadFooterAdContainerSetupFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.DIMEN, "read_ad_minimum"),
        methodCall("Landroid/view/View;->setMinimumHeight(I)V"),
    ),
)