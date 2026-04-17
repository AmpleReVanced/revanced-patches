package app.revanced.patches.kakaotalk.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.util.hasMethodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object OpenChatFeedAdLoadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "OpenChatTabFeedAdViewHolder.kt" &&
                method.parameterTypes.size == 2 &&
                method.hasMethodCall(
                    "Lcom/kakao/adfit/ads/feed/FeedAdLoader;",
                    "loadAd",
                    "Z"
                )
    }
)

internal object OpenChatFeedAdRenderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IGET_OBJECT
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "OpenChatTabFeedAdViewHolder.kt" &&
                method.parameterTypes.size == 3 &&
                method.hasMethodCall(
                    "Lcom/kakao/adfit/ads/feed/FeedAdLayout;",
                    "setFeedAd",
                    "V"
                )
    }
)
