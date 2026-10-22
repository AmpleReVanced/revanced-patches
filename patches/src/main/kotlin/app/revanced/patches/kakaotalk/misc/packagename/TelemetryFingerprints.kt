package app.revanced.patches.kakaotalk.misc.packagename

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.StringComparisonType.CONTAINS
import app.morphe.patcher.StringComparisonType.STARTS_WITH
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object FirebaseModelHeadersFingerprint : Fingerprint(
    strings = listOf("X-Android-Package", "X-Android-Cert", "x-goog-api-key"),
    filters = listOf(
        string("X-Android-Package"),
        methodCall("Landroid/content/Context;->getPackageName()Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "CustomModelDownloadService.java" },
)

internal object PlacesGrpcHeadersFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
    strings = listOf("X-Goog-FieldMask", "X-Android-Package", "X-Android-Cert"),
    filters = listOf(
        string("X-Android-Package"),
        methodCall("Landroid/content/Context;->getPackageName()Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile?.startsWith("com.google.android.libraries.places:places@@") == true },
)

internal object PlacesHttpHeadersFingerprint : Fingerprint(
    parameters = listOf(),
    strings = listOf("X-Android-Package", "X-Android-Cert"),
    filters = listOf(
        methodCall("Landroid/content/Context;->getPackageName()Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile?.startsWith("com.google.android.libraries.places:places@@") == true },
)

internal object AdFitMatrixInitializerFingerprint : Fingerprint(
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
    strings = listOf(string("aem-kakao-collector.onkakao.net/12221", CONTAINS)),
)

internal object MlKitTransportConstructorFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Landroid/content/Context;", "L"),
    strings = listOf("json"),
    custom = { _, classDef -> classDef.sourceFile?.contains("mlkit") == true },
)

internal fun mlKitTransportSendFingerprint() = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L"),
    returnType = "V",
    filters = listOf(methodCall(parameters = listOf("L"), returnType = "V", opcode = Opcode.INVOKE_INTERFACE)),
)

internal object OnePassLoggerInitializerFingerprint : Fingerprint(
    parameters = listOf("Lorg/json/JSONObject;"),
    returnType = "V",
    strings = listOf("deviceLogConf", "deviceLogCollect", "sendConstraint"),
    custom = { _, classDef -> classDef.sourceFile == "OPLoggerManager.java" },
)

internal object AnalyticsRegistrarFingerprint : Fingerprint(
    parameters = listOf(),
    returnType = "Ljava/util/List;",
    strings = listOf("fire-analytics"),
    custom = { _, classDef -> classDef.sourceFile?.startsWith("com.google.android.gms:play-services-measurement-api@@") == true },
)

internal object AnalyticsCollectionFingerprint : Fingerprint(
    parameters = listOf(),
    returnType = "I",
    strings = listOf("firebase_analytics_collection_enabled"),
    custom = { _, classDef -> classDef.sourceFile?.startsWith("com.google.android.gms:play-services-measurement-impl@@") == true },
)

internal object AnalyticsServiceConnectionFingerprint : Fingerprint(
    parameters = listOf(),
    returnType = "V",
    strings = listOf(
        string("com.google.android.gms.measurement.START"),
        string("Unable to use remote or local measurement implementation.", STARTS_WITH),
    ),
    custom = { _, classDef -> classDef.sourceFile?.startsWith("com.google.android.gms:play-services-measurement-impl@@") == true },
)

internal object FirebaseSessionSendFingerprint : Fingerprint(
    parameters = listOf("L"),
    returnType = "V",
    strings = listOf("FIREBASE_APPQUALITY_SESSION"),
    custom = { _, classDef -> classDef.sourceFile == "EventGDTLogger.kt" },
)

internal object FirebaseDeliveryMetricsSendFingerprint : Fingerprint(
    parameters = listOf("L", "Landroid/content/Intent;", "L"),
    returnType = "V",
    strings = listOf("FCM_CLIENT_EVENT_LOGGING"),
    custom = { _, classDef -> classDef.sourceFile == "MessagingAnalytics.java" },
)

internal object CrashlyticsReportSendFingerprint : Fingerprint(
    parameters = listOf("L", "Z"),
    returnType = "L",
    custom = { _, classDef -> classDef.sourceFile == "DataTransportCrashlyticsReportSender.java" },
)

internal object TaskForResultFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "L",
    filters = listOf(opcode(Opcode.NEW_INSTANCE)),
    custom = { _, classDef -> classDef.sourceFile?.startsWith("com.google.android.gms:play-services-tasks@@") == true },
)
