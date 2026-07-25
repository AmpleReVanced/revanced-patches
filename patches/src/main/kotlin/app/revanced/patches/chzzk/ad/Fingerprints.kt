package app.revanced.patches.chzzk.ad

import app.morphe.patcher.Fingerprint

private const val AD_ENTER_PLAYER_CLASS =
    "Lcom/navercorp/game/android/community/core/feature/feature/player/ad/AdEnterPlayer;"

private const val LIVE_PLAYER_EVENT_AD_CLASS =
    "Lcom/navercorp/game/android/community/core/feature/feature/player/data/LivePlayerData\$Event\$AD;"

private const val PRISM_SOURCE_CLASS = "Lcom/naver/prismplayer/Source;"

private const val PLAYABLE_AD_PARAMS_CLASS =
    "Lcom/navercorp/game/android/community/data/core/entity/player/PlayableAdParams;"

private const val CREATOR_HUB_CARD_CLASS = "Lcom/navercorp/shortform/sdk/data/dto/CreatorHubCard;"

private const val FEED_AD_DATA_CLASS = "Lcom/navercorp/shortform/sdk/data/dto/FeedAdData;"

private const val SHORT_FORM_AD_CARD_MODEL_CLASS =
    "Lcom/navercorp/shortform/sdk/data/model/ShortFormAdCardModel;"

internal object AdEnterPlayerFingerprint : Fingerprint(
    definingClass = AD_ENTER_PLAYER_CLASS,
    returnType = "V",
    parameters = listOf(LIVE_PLAYER_EVENT_AD_CLASS),
    strings = listOf("adRequest"),
)

internal object ApplyPlayerAdParamsFingerprint : Fingerprint(
    returnType = PRISM_SOURCE_CLASS,
    parameters = listOf(PRISM_SOURCE_CLASS, PLAYABLE_AD_PARAMS_CLASS),
    strings = listOf("adParams", "nmp_aos", "calp"),
)

internal object MapClipAdCardFingerprint : Fingerprint(
    returnType = SHORT_FORM_AD_CARD_MODEL_CLASS,
    parameters = listOf(CREATOR_HUB_CARD_CLASS, "J", "Ljava/lang/String;", FEED_AD_DATA_CLASS),
    strings = listOf("mapAdCardModel: onFailure "),
)