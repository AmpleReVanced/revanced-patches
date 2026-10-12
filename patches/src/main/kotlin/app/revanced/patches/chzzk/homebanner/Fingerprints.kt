package app.revanced.patches.chzzk.homebanner

import app.morphe.patcher.Fingerprint

internal object StreamingHomeBannersFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("StreamingHomeBanners(banners="),
)

internal object FootballCampaignFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("FootballCampaign(exposure="),
)

internal object TopicBannerRowsFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("topic_imageBannerListItem_", "topic_spacialEventBannerListItem_"),
)