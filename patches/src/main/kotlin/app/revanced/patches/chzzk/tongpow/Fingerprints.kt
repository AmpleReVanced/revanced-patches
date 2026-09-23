package app.revanced.patches.chzzk.tongpow

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import app.revanced.util.parameterTypeNames
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val CHAT_VIEW_MODEL_CLASS =
    "Lcom/navercorp/game/android/community/app/ui/overlayplayerend/live/streaming/chat/StreamingChatViewModel;"
private const val CHAT_POPUP_PACKAGE =
    "Lcom/navercorp/game/android/community/app/ui/overlayplayerend/live/streaming/chat/popup/"
private const val TONG_POW_EVENT_CLASS =
    "Lcom/navercorp/game/android/community/ui/common/feature/session/SessionEvent\$TongPowEvent;"
private const val CLAIM_EVENT_CLASS =
    "Lcom/navercorp/game/android/community/app/ui/overlayplayerend/live/streaming/chat/popup/ChatTongPowEvent\$ShowTongPowPopupInfo;"
private const val CLAIM_SERVICE_CLASS =
    "Lcom/navercorp/game/android/community/data/core/service/tongpow/ApiTongPowService\$ApiService;"

internal object TongPowChatEventFingerprint : Fingerprint(
    definingClass = CHAT_VIEW_MODEL_CLASS,
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Lkotlin/Unit;",
    custom = { method, _ ->
        method.implementation?.instructions?.let { instructions ->
            instructions.any { it.getReference<TypeReference>()?.type == TONG_POW_EVENT_CLASS } &&
                instructions.any { it.getReference<MethodReference>()?.definingClass == CLAIM_EVENT_CLASS }
        } == true
    },
)

internal object TongPowManualClaimFingerprint : Fingerprint(
    name = "invoke",
    parameters = emptyList(),
    returnType = "Ljava/lang/Object;",
    custom = { method, classDef ->
        classDef.type.startsWith(CHAT_POPUP_PACKAGE) &&
            "Lkotlin/jvm/functions/Function0;" in classDef.interfaces &&
            method.implementation?.instructions?.any { instruction ->
                instruction.getReference<MethodReference>()?.let { reference ->
                    reference.definingClass == CLAIM_SERVICE_CLASS &&
                        reference.parameterTypeNames == listOf("Ljava/lang/String;", "Ljava/lang/String;")
                } == true
            } == true
    },
)