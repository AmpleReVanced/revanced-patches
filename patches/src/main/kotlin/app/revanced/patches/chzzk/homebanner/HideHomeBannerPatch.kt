package app.revanced.patches.chzzk.homebanner

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.chzzk.common.utils.forceBooleanGetterFalse
import app.revanced.patches.chzzk.common.utils.forceObjectGetterNull
import app.revanced.patches.chzzk.shared.Constants.COMPATIBILITY_CHZZK

@Suppress("unused")
val hideHomeBannerPatch = bytecodePatch(
    name = "Hide home banners",
    description = "Hides promotional banners on the CHZZK home recommend tab, including the top " +
        "banner carousel and the in-feed event and image banners.",
) {
    compatibleWith(COMPATIBILITY_CHZZK)

    execute {
        StreamingHomeBannersFingerprint.classDef.forceObjectGetterNull("banners")
        TopicImageBannerRowFingerprint.method.returnEarly()
        TopicSpecialEventBannerRowFingerprint.method.returnEarly()
        FootballCampaignFingerprint.classDef.forceBooleanGetterFalse("exposure")
    }
}