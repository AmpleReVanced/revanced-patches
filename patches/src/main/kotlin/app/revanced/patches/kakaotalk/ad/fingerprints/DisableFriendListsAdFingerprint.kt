package app.revanced.patches.kakaotalk.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.revanced.util.hasMethodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object FriendListChipBizBoardBindFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "FriendListChipBizBoardAdViewHolder.kt" &&
                method.parameterTypes.size == 2 &&
                method.hasMethodCall(
                    "Lcom/kakao/adfit/ads/talk/TalkNativeAdBinder;",
                    "setPrivateAdEventListener",
                    "V"
                )
    }
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
