package app.revanced.patches.kakaotalk.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object LoadFocusAdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Lcom/kakao/adfit/ads/focus/FocusAdLoader\$OnAdLoadListener;"),
    returnType = "Z",
    strings = listOf("listener", " owner is destroyed.", " loading is already started.", "Request Focus AD", " loading is started.", "Focus ad is cached. [id = "),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IF_NE,
    )
)