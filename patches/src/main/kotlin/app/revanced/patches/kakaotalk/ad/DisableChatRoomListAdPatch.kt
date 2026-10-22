package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ad.fingerprints.CecallFolderBizBoardEnabledFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.ChatListAdItemFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.ChatListBannerAdItemFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.ChatListBizBoardEnabledFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.ChatListGlobalAdEnabledFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.CommonChatRoomListSubmitFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.GlobalChatListAdItemFingerprint
import app.revanced.patches.kakaotalk.misc.extension.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.ListFilterGroup
import app.revanced.patches.kakaotalk.shared.filterAdapterItems
import app.revanced.patches.kakaotalk.shared.setListFilter

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/DisableChatRoomListAdPatch;"

@Suppress("unused")
val disableChatRoomListAdPatch = bytecodePatch(
    name = "Disable chat room list ad",
    description = "Disables chat room list and Call filter ads, including reserved ad rows.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(sharedExtensionPatch)

    execute {
        ChatListBizBoardEnabledFingerprint.method.returnEarly(false)
        ChatListGlobalAdEnabledFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """.trimIndent()
        )
        CecallFolderBizBoardEnabledFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """.trimIndent()
        )
        setListFilter(
            EXTENSION_CLASS,
            listOf(
                ListFilterGroup(
                    listOf(
                        ChatListAdItemFingerprint.originalClassDef.type,
                        ChatListBannerAdItemFingerprint.originalClassDef.type,
                        GlobalChatListAdItemFingerprint.originalClassDef.type,
                    ),
                ),
            ),
        )
        filterAdapterItems(
            CommonChatRoomListSubmitFingerprint.classDef,
            EXTENSION_CLASS,
            listOf(CommonChatRoomListSubmitFingerprint.originalMethod),
        )
    }
}
