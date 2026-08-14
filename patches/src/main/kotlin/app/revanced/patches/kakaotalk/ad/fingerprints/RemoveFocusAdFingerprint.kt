package app.revanced.patches.kakaotalk.ad.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object LoadFocusAdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Lcom/kakao/adfit/ads/focus/FocusAdLoader\$OnAdLoadListener;"),
    returnType = "Z",
    strings = listOf(
        " owner is destroyed.",
        " loading is already started.",
        "Request Focus AD",
        " loading is started.",
        "Focus ad is cached. [id = ",
    ),
)
