package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.Fingerprint

internal object IntroAdBannerFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf(
        "MOBILE_INTRO_CINEMA_BANNER",
        "MOBILE_INTRO_IMAGE_BANNER",
        "MOBILE_INTRO_VIDEO_BANNER",
    ),
)