package app.revanced.patches.kakaotalk.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object FriendListChipBizBoardBindFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "L"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/kakao/adfit/ads/talk/TalkNativeAdBinder;",
            name = "setPrivateAdEventListener",
            returnType = "V",
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "FriendListChipBizBoardAdViewHolder.kt" }
)

internal object BirthdayFriendsBizBoardBindFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Lcom/kakao/adfit/ads/media/NativeAdBinder;"),
    returnType = "V",
    custom = { _, classDef -> classDef.sourceFile == "FriendTabBirthdayFriendsBizBoardAdViewModel.kt" }
)

internal object FriendTabGlobalAdModelFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.NEW_INSTANCE,
    ),
    custom = { _, classDef -> classDef.sourceFile == "FriendTabGlobalAdViewModel.kt" }
)
