package app.revanced.patches.navermap.liveupdate

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private val navigationServices = setOf(
    "Lcom/naver/map/core/navigation/NaviForegroundService;",
    "Lcom/naver/map/core/walknavigation/service/WalkNaviForegroundService;",
    "Lcom/naver/map/core/pubtransalarm/PubtransAlarmService;",
)

internal object OngoingActivitySupportFingerprint : Fingerprint(
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Z",
    strings = listOf("com.samsung.feature.nowbar"),
)

internal class NavigationOngoingActivitySupportFingerprint(supportMethod: MethodReference) : Fingerprint(
    filters = listOf(
        methodCall(smali = supportMethod.toString()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
    custom = { method, classDef ->
        classDef.type in navigationServices || method.implementation?.instructions?.any {
            it.getReference<StringReference>()?.string ==
                "com.naver.map.core.pubtransalarm.ongoing.delete_intent_action"
        } == true
    },
)

internal object OngoingActivityExtrasFingerprint : Fingerprint(
    strings = listOf("android.ongoingActivityNoti.style", "ongoingActivity"),
    custom = { method, _ ->
        method.parameterTypes.size in 3..4 &&
            method.parameterTypes[1] == "Landroid/content/Context;" &&
            (method.parameterTypes.size == 3 || method.parameterTypes[3] == "Landroid/app/PendingIntent;") &&
            method.returnType == method.parameterTypes[0].toString()
    },
)

internal class NotificationBuildFingerprint(builderClass: String) : Fingerprint(
    definingClass = builderClass,
    parameters = emptyList(),
    returnType = "Landroid/app/Notification;",
)