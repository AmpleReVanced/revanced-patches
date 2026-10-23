package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType.CONTAINS
import app.morphe.patcher.string

internal object LiveBannerDriverFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    strings = listOf(string("LiveAdCoupleBannerFragment", CONTAINS), string("LiveAdBannerFragment", CONTAINS)),
)

internal object CompanionBannerViewCreatedFingerprint : Fingerprint(
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
    returnType = "V",
    custom = { _, classDef ->
        classDef.type ==
            "Lkr/co/nowcom/mobile/afreeca/player/vod/vod/adviews/presenter/banner/AfAdCompanionBannerFragment;"
    },
)

internal object SearchAdViewCreatedFingerprint : Fingerprint(
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
    returnType = "V",
    custom = { _, classDef ->
        classDef.type ==
            "Lkr/co/nowcom/mobile/afreeca/advertisement/a1/search/presenter/SearchMiddleAdFragment;"
    },
)