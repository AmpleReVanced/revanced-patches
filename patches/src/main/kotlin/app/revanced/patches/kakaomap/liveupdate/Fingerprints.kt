package app.revanced.patches.kakaomap.liveupdate

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private val journeyNotificationSourceFiles = setOf(
    "BicycleGuidanceService.kt",
    "NaviService.kt",
    "PubtransGuidanceNotification.kt",
)

private fun hasString(method: Method, value: String) =
    method.implementation?.instructions?.any { instruction ->
        instruction.getReference<StringReference>()?.string == value
    } == true

internal object JourneyNotificationBuildFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            name = "build",
            parameters = emptyList(),
            returnType = "Landroid/app/Notification;",
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile in journeyNotificationSourceFiles },
)

internal object PubtransRemoteViewsSetTextFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("I", "Ljava/lang/String;", "Z", "Z", "Z"),
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Landroid/widget/RemoteViews;",
            name = "setTextViewText",
            parameters = listOf("I", "Ljava/lang/CharSequence;"),
            returnType = "V",
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "PubtransRemoteViewsWrapper.kt" },
)

internal object PubtransRemoteViewsContentIntentFingerprint : Fingerprint(
    parameters = listOf("I", "Landroid/app/PendingIntent;"),
    returnType = "V",
    custom = { _, classDef -> classDef.sourceFile == "PubtransRemoteViewsWrapper.kt" },
)

internal object PubtransRemoteViewsNavigationActionsFingerprint : Fingerprint(
    parameters = listOf(
        "Landroid/content/Context;", "I", "I",
        "Ljava/lang/Integer;", "Ljava/lang/Integer;", "Ljava/lang/Integer;",
    ),
    returnType = "V",
    strings = listOf("NOTIFICATION_ACTION_PREV", "NOTIFICATION_ACTION_NEXT"),
    custom = { _, classDef -> classDef.sourceFile == "PubtransRemoteViewsWrapper.kt" },
)

internal object PubtransJourneyTerminateFingerprint : Fingerprint(
    parameters = emptyList(),
    returnType = "V",
    strings = listOf("[start] showTerminatingNotification()"),
    custom = { _, classDef -> classDef.sourceFile == "PubtransGuidanceNotification.kt" },
)

internal object PubtransJourneyProgressFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "PubtransGuidanceNotification.kt" &&
            method.parameterTypes.map(CharSequence::toString) == listOf("I", "I", "I", "Z", "Z") &&
            method.returnType == "I" &&
            hasString(method, "[start] showNotification()")
    },
)

internal object PubtransNotificationGenerationFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "PubtransGuidanceNotification.kt" &&
            method.parameterTypes.map(CharSequence::toString).let { parameters ->
                parameters.size in 8..9 &&
                    parameters[0] == "Landroid/content/Context;" &&
                    parameters[1].startsWith("L") &&
                    parameters.subList(2, 6) == listOf("I", "I", "I", "Z") &&
                    parameters.takeLast(2) == listOf("Z", "Z") &&
                    (parameters.size == 8 || parameters[6] == "Ljava/lang/String;")
            } &&
            method.returnType == "V" &&
            hasString(method, "[start] generateNotification()")
    },
)

internal object PubtransJourneyStartFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "PubtransGuidanceNotification.kt" &&
            method.parameterTypes.isEmpty() &&
            method.returnType == "V" &&
            hasString(method, "[start] showDepartureStepNotification()")
    },
)

internal object PubtransJourneyCompleteFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "PubtransGuidanceNotification.kt" &&
            method.parameterTypes.map(CharSequence::toString) == listOf("Z") &&
            method.returnType == "V" &&
            hasString(method, "[start] showArrivalStepNotification()")
    },
)

internal object PubtransLocationUpdateFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/map/route/pubtrans/guidance/PubtransGuidanceService;",
    name = "onNewCoordinate",
    parameters = listOf("Landroid/location/Location;"),
    returnType = "V",
)

internal object PubtransSectionDistanceRatioFingerprint : Fingerprint(
    name = "getCurrentSectionTraveledDistanceRatio",
    parameters = emptyList(),
    returnType = "D",
    custom = { _, classDef -> classDef.sourceFile == "PubtransTraveler.kt" },
)

internal object PubtransLocationWarmupFingerprint : Fingerprint(
    filters = listOf(
        literal(5, opcodes = listOf(Opcode.CONST_4)),
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "PubtransTraveler.kt" &&
            method.name == "handleNewCoordinate" &&
            method.parameterTypes.map(CharSequence::toString) ==
            listOf("Landroid/location/Location;") &&
            method.returnType == "V" &&
            hasString(method, "[PROCESS] Wi-Fi has not scanned yet, ignore coordinate.")
    },
)

internal object BusAndWalkLocationMatchFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "BusAndWalkTraveler.kt" &&
            method.name == "handleNewCoordinate" &&
            method.parameterTypes.map(CharSequence::toString) ==
            listOf("Landroid/location/Location;") &&
            method.returnType == "V"
    },
)