package app.revanced.patches.kakaotalk.ads.fingerprints

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
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    strings = listOf("item"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_SUPER,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "FriendTabBirthdayFriendsBizBoardAdViewHolder.kt" &&
                method.parameterTypes.size == 1 &&
                method.hasMethodCall(
                    "Lcom/kakao/adfit/ads/media/NativeAdBinder;",
                    "bind",
                    "V"
                )
    }
)