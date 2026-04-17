package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ads.fingerprints.OpenChatFeedAdLoadFingerprint
import app.revanced.patches.kakaotalk.ads.fingerprints.OpenChatFeedAdRenderFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableOpenChatFeedAdPatch = bytecodePatch(
    name = "Disable OpenChat feed ad",
    description = "Disables the OpenChat tab feed ad load and render paths.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        OpenChatFeedAdLoadFingerprint.method.returnEarly()
        OpenChatFeedAdRenderFingerprint.method.returnEarly()
    }
}