package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ad.fingerprints.OpenChatFeedAdItemFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.OpenChatFeedAdapterFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.OpenChatFeedAdLoadFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.OpenChatFeedAdRenderFingerprint
import app.revanced.patches.kakaotalk.misc.extension.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.ListFilterGroup
import app.revanced.patches.kakaotalk.shared.filterAdapterItems
import app.revanced.patches.kakaotalk.shared.setListFilter

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/DisableOpenChatFeedAdPatch;"

@Suppress("unused")
val disableOpenChatFeedAdPatch = bytecodePatch(
    name = "Disable OpenChat feed ad",
    description = "Disables the OpenChat tab feed ad load and render paths.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(sharedExtensionPatch)

    execute {
        OpenChatFeedAdLoadFingerprint.method.returnEarly()
        OpenChatFeedAdRenderFingerprint.method.returnEarly()
        setListFilter(
            EXTENSION_CLASS,
            listOf(ListFilterGroup(listOf(OpenChatFeedAdItemFingerprint.originalClassDef.type))),
        )
        filterAdapterItems(OpenChatFeedAdapterFingerprint.classDef, EXTENSION_CLASS)
    }
}