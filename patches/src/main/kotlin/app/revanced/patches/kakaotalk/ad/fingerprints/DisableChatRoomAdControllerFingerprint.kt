package app.revanced.patches.kakaotalk.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object OpenLinkChatAdControllerLoadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(methodCall(definingClass = "Lcom/kakao/adfit/ads/bizboard/BizBoardAdView;", name = "loadAd", returnType = "V")),
    custom = { _, classDef -> classDef.sourceFile == "OpenLinkChatAdController.kt" }
)