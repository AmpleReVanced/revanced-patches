package app.revanced.patches.chzzk.homebanner

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

internal object StreamingHomeBannersFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("StreamingHomeBanners(banners="),
)

internal object FootballCampaignFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("FootballCampaign(exposure="),
)

internal object TopicBannerRowsFingerprint : Fingerprint(
    filters = listOf(
        string("topic_imageBannerListItem_"),
        string("topic_spacialEventBannerListItem_"),
    ),
)