package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ad.fingerprints.ChatListAdItemFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.CommonChatRoomListSubmitFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.OpenChatAdItemFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.OpenChatRoomListAdapterFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.OpenChatTabBizBoardUpdateFingerprint
import app.revanced.patches.kakaotalk.misc.extension.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.ListFilterGroup
import app.revanced.patches.kakaotalk.shared.filterAdapterItems
import app.revanced.patches.kakaotalk.shared.setListFilter

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/RemoveOlkChatRoomListAdPatch;"

@Suppress("unused")
val removeOlkChatRoomListAdPatch = bytecodePatch(
    name = "Remove OpenLink chat room list ad",
    description = "Removes the OpenLink chat room list ad.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(sharedExtensionPatch)

    execute {
        OpenChatTabBizBoardUpdateFingerprint.instructionMatches[1].getMethodCalled().returnEarly(false)
        setListFilter(
            EXTENSION_CLASS,
            listOf(
                ListFilterGroup(
                    listOf(
                        OpenChatAdItemFingerprint.originalClassDef.type,
                        ChatListAdItemFingerprint.originalClassDef.type,
                    ),
                ),
            ),
        )
        filterAdapterItems(
            OpenChatRoomListAdapterFingerprint.classDef,
            EXTENSION_CLASS,
            listOf(CommonChatRoomListSubmitFingerprint.originalMethod),
        )
    }
}
