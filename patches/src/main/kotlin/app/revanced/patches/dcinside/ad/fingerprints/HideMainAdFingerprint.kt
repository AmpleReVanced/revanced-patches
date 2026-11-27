package app.revanced.patches.dcinside.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.string

internal object HideMainAdFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Landroid/widget/FrameLayout;", "L"),
    returnType = "V",
    filters = listOf(string("ad_banner_main_center", comparison = StringComparisonType.ENDS_WITH)),
)