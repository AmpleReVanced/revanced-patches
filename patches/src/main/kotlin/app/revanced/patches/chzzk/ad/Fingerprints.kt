package app.revanced.patches.chzzk.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType.CONTAINS
import app.morphe.patcher.string

private const val PRISM_SOURCE_CLASS = "Lcom/naver/prismplayer/Source;"

internal object AdEnterPlayerFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L"),
    strings = listOf("디버깅: 이미광고재생중", "디버깅: 이미광고요청중"),
)

internal object ApplyPlayerAdParamsFingerprint : Fingerprint(
    returnType = PRISM_SOURCE_CLASS,
    parameters = listOf(PRISM_SOURCE_CLASS, "L"),
    strings = listOf("nmp_aos", "calp"),
)

internal object MapRecommendedCardsFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    strings = listOf(string("mapAdCardModel: onFailure ", CONTAINS)),
)