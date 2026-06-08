package app.revanced.patches.chzzk.p2p

import app.morphe.patcher.Fingerprint

internal const val PEER_NETWORK_CONFIGURATION_CLASS =
    "Lcom/naver/prismplayer/player/PeerNetworkConfiguration;"

internal object PeerNetworkConfigurationFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf(
        "PeerNetworkConfiguration(enabled=",
        ", excludedEngineVersions="
    ),
    custom = { method, classDef ->
        classDef.type == PEER_NETWORK_CONFIGURATION_CLASS &&
            method.parameterTypes.isEmpty()
    }
)