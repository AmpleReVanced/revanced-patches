package app.revanced.patches.kakaotalk.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.util.hasMethodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object OpenLinkChatAdControllerLoadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "OpenLinkChatAdController.kt" &&
                method.parameterTypes.isEmpty() &&
                method.hasMethodCall(
                    "Lcom/kakao/adfit/ads/bizboard/BizBoardAdView;",
                    "loadAd",
                    "V"
                )
    }
)