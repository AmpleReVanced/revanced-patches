package app.revanced.patches.kakaotalk.misc.packagename

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object CheckPackageNameFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
    strings = listOf("com_kakao_talk", "_", ".", "Check failed."),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/Context;",
            name = "getPackageName",
            parameters = listOf(),
            returnType = "Ljava/lang/String;",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "AppHelper.kt" }
)

internal object GetInstallSourceInfoFingerprint : Fingerprint(
    name = "invokeSuspend",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    strings = listOf("com.kakao.talk"),
    custom = { _, classDef ->
        classDef.sourceFile == "Initializer.kt" && classDef.type.contains("$")
    },
)

internal object EmoticonDecryptContextFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        methodCall("Landroid/content/Context;->getApplicationContext()Landroid/content/Context;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(
            type = "Landroid/content/Context;",
            opcode = Opcode.SPUT_OBJECT,
            location = MatchAfterImmediately(),
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "EmoticonDecrypt.kt" },
)

internal object KakaoSdkAgentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;", "L", "Ljava/lang/String;"),
    returnType = "Ljava/lang/String;",
    strings = listOf("android_pkg", "app_ver", "sdk_type"),
    custom = { _, classDef -> classDef.sourceFile == "Utility.kt" },
)

internal object KakaoSdkExtrasFingerprint : Fingerprint(
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;"),
    strings = listOf("keyHash", "KA"),
    filters = listOf(
        string("appPkg"),
        methodCall(
            "Landroid/content/Context;->getPackageName()Ljava/lang/String;",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "Utility.kt" },
)

internal object FirebaseInstallationHeadersFingerprint : Fingerprint(
    parameters = listOf("Ljava/net/URL;", "Ljava/lang/String;"),
    returnType = "Ljava/net/HttpURLConnection;",
    strings = listOf("X-Android-Cert", "x-goog-api-key"),
    filters = listOf(
        string("X-Android-Package"),
        methodCall(
            "Ljava/net/URLConnection;->addRequestProperty(Ljava/lang/String;Ljava/lang/String;)V",
            location = MatchAfterImmediately(),
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "FirebaseInstallationServiceClient.java" },
)

internal object FidoDiagnosticPropertiesFingerprint : Fingerprint(
    parameters = listOf(),
    returnType = "Ljava/lang/String;",
    strings = listOf("lab", "pan", "dip"),
    filters = listOf(methodCall("Lorg/json/JSONObject;->toString()Ljava/lang/String;")),
    custom = { _, classDef -> classDef.sourceFile == "ExtraProperties.kt" },
)

internal object FaceprintInitFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/String;", "Landroid/content/Context;", "Landroid/app/Activity;"),
    returnType = "I",
    strings = listOf("kii_Data/Classifier", "kii_Data/Mask.raw"),
    filters = listOf(
        methodCall(
            parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;") + List(5) { listOf("I", "[B") }.flatten(),
            returnType = "I",
            opcode = Opcode.INVOKE_VIRTUAL_RANGE,
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "FaceDetect.java" },
)

internal object KakaoTvTalkLinkFingerprint : Fingerprint(
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
    returnType = "V",
    strings = listOf("android.intent.action.VIEW"),
    filters = listOf(
        string("com.kakao.talk"),
        methodCall("Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "IntentUtil.kt" },
)

internal object PayPlatformPackageFingerprint : Fingerprint(
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("com.kakao.talk", "com.kakaopay.app"),
    filters = listOf(fieldAccess("Landroid/content/pm/ApplicationInfo;->packageName:Ljava/lang/String;", opcode = Opcode.IGET_OBJECT)),
    custom = { _, classDef -> classDef.sourceFile == "PayPlatform.kt" },
)

internal object PayAuthTalkPackageFingerprint : Fingerprint(
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Z",
    strings = listOf("com.kakao.talk"),
    filters = listOf(
        methodCall("Landroid/content/Context;->getPackageName()Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "PayAuthContextExtension.kt" },
)

internal object KakaoTvApplicationPackageFingerprint : Fingerprint(
    parameters = listOf(),
    returnType = "Ljava/lang/String;",
    strings = listOf("com.kakao.talk", "talk", "com.kakao.story", "story"),
    filters = listOf(fieldAccess("Landroid/content/pm/ApplicationInfo;->packageName:Ljava/lang/String;", opcode = Opcode.IGET_OBJECT)),
    custom = { _, classDef -> classDef.sourceFile == "ApplicationInfo.kt" },
)