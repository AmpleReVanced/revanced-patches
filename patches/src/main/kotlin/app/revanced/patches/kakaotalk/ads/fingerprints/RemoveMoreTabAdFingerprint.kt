package app.revanced.patches.kakaotalk.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object AddSectionToMoreTabUIFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    strings = listOf(
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.KakaoPayUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.WeatherUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.KakaoNowUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.GridServiceUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.AdBigUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.AdLocalUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.MoreTabConfiguration",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.GamePlayUiModel",
        "null cannot be cast to non-null type kotlin.collections.List<com.kakao.talk.moretab.domain.model.MoreTabFeature>",
        "call to 'resume' before 'invoke' with coroutine",
    ),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_EQZ
    )
)

internal object AdBigUIModelFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    returnType = "Ljava/lang/String;",
    strings = listOf("AdBig(uiModel="),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.CONST_STRING,
    )
)