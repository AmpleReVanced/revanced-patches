package app.revanced.patches.kakaotalk.ads.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val addSectionToMoreTabUIFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Ljava/lang/Object;")
    returns("Ljava/lang/Object;")
    strings(
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.KakaoPayUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.WalletUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.WeatherUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.KakaoNowUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.TalkManualUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.ServiceGroupUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.WalletBannerUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.AdBigUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.AdLocalUiModel",
        "null cannot be cast to non-null type com.kakao.talk.moretab.ui.model.ServiceShortcutUiModel",
        "null cannot be cast to non-null type kotlin.Boolean",
    )
    opcodes(
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
    )
}

internal val adBigUIModelFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    parameters()
    returns("Ljava/lang/String;")
    strings(
        "AdBig(uiModel=",
    )
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.CONST_STRING,
    )
    custom { method, classDef -> method.name == "toString" }
}