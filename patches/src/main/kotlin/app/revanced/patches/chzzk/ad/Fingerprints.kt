package app.revanced.patches.chzzk.ad

import app.morphe.patcher.Fingerprint

private const val AD_ENTER_PLAYER_CLASS =
    "Lcom/navercorp/game/android/community/core/feature/feature/player/ad/AdEnterPlayer;"

private const val LIVE_PLAYER_EVENT_AD_CLASS =
    "Lcom/navercorp/game/android/community/core/feature/feature/player/data/LivePlayerData\$Event\$AD;"

private const val PRISM_SOURCE_CLASS = "Lcom/naver/prismplayer/Source;"

private const val PLAYABLE_AD_PARAMS_CLASS =
    "Lcom/navercorp/game/android/community/data/core/entity/player/PlayableAdParams;"

internal object AdEnterPlayerFingerprint : Fingerprint(
    definingClass = AD_ENTER_PLAYER_CLASS,
    returnType = "V",
    parameters = listOf(LIVE_PLAYER_EVENT_AD_CLASS),
)

internal object ApplyPlayerAdParamsFingerprint : Fingerprint(
    returnType = PRISM_SOURCE_CLASS,
    parameters = listOf(PRISM_SOURCE_CLASS, PLAYABLE_AD_PARAMS_CLASS),
    strings = listOf("nmp_aos", "calp"),
)

internal object MapRecommendedCardsFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    strings = listOf("mapAdCardModel: onFailure "),
)