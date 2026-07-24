package app.revanced.patches.chzzk.homebanner

import app.morphe.patcher.Fingerprint

private const val LAZY_LIST_SCOPE_CLASS = "Landroidx/compose/foundation/lazy/LazyListScope;"

private const val IMAGE_BANNER_COMPONENT_CLASS =
    "Lcom/navercorp/game/android/community/data/mobile/entity/recommendation/TopicSlotComponent\$ImageBannerComponent;"

internal object StreamingHomeBannersFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("StreamingHomeBanners(banners="),
)

internal object FootballCampaignFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("FootballCampaign(exposure="),
)

internal object TopicImageBannerRowFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        LAZY_LIST_SCOPE_CLASS,
        IMAGE_BANNER_COMPONENT_CLASS,
        "I",
        "Lkotlin/jvm/functions/Function1;",
    ),
    strings = listOf("topic_imageBannerListItem_"),
)

internal object TopicSpecialEventBannerRowFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("topic_spacialEventBannerListItem_"),
)