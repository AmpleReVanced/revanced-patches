package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ad.fingerprints.BirthdayFriendsBizBoardBindFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.FriendListChipBizBoardBindFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.FriendTabGlobalAdModelFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableFriendListsAdPatch = bytecodePatch(
    name = "Disable Friend Lists ad",
    description = "Disables the friend tab BizBoard and global-region ads in KakaoTalk.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        FriendListChipBizBoardBindFingerprint.method.returnEarly()
        BirthdayFriendsBizBoardBindFingerprint.method.returnEarly()
        FriendTabGlobalAdModelFingerprint.method.returnEarly(null)
    }
}
