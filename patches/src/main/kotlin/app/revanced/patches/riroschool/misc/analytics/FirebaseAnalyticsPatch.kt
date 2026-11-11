package app.revanced.patches.riroschool.misc.analytics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.revanced.patches.riroschool.shared.requireRiroTarget
import app.revanced.patches.riroschool.shared.riroCompatibility
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val FIREBASE_INTRO = "Lcom/rirosoft/riroschool/views/activity/IntroActivity;"
private const val FIREBASE_SERVICE = "Lcom/rirosoft/riroschool/service/MyFirebaseMessagingService;"
private const val ANDROID_XML = "http://schemas.android.com/apk/res/android"

private val telemetryTransportBytecodePatch = bytecodePatch {
    compatibleWith(riroCompatibility)
    execute {
        requireRiroTarget(packageMetadata)
        editFirebaseMethod("Li3/r;", "a", listOf("Lf3/d;", "Lf3/i;"), "V", ::firebaseTransportIngress)
        editFirebaseMethod("Lo3/i;", "a", listOf("Li3/j;", "I"), "V") { original ->
            MutableMethod(original).also(::dropQueuedTelemetry)
        }
        editFirebaseMethod("Lo3/c;", "a", listOf("Li3/j;", "I", "Z"), "V") { original ->
            require(firebaseMethodIndex(original, "Landroid/app/job/JobScheduler;", "schedule") >= 0)
            firebaseBody(original, 4, "return-void")
        }

    }
}

internal val disableTelemetryTransportPatch = resourcePatch {
    compatibleWith(riroCompatibility)
    dependsOn(telemetryTransportBytecodePatch)
    execute {
        requireRiroTarget(packageMetadata)
        document("AndroidManifest.xml").use { manifest -> disableTransportComponents(manifest) }
    }
}

private val firebaseAnalyticsBytecodePatch = bytecodePatch(

    description = "Disable Firebase telemetry and token registration. Push is unavailable; also disables shared app DataTransport telemetry.",
    default = true,
) {
    compatibleWith(riroCompatibility)
    dependsOn(disableTelemetryTransportPatch)
    execute {
        requireRiroTarget(packageMetadata)
        editFirebaseMethod(FIREBASE_INTRO, "onCreate", listOf("Landroid/os/Bundle;"), "V") { original ->
            MutableMethod(original).also(::removeFirebaseStartup)
        }
        editFirebaseMethod(FIREBASE_INTRO, "c", emptyList(), "V", ::firebaseProgression)
        editFirebaseMethod("Lg6/i;", "f", listOf("Landroid/content/Context;"), "Ljava/lang/String;", ::firebaseEmptyToken)
        editFirebaseMethod("Lg6/h;", "p", listOf("Landroid/content/Context;", "Ljava/lang/String;"), "V") { original ->
            require(firebaseStringIndex(original, "APP_FCM_TOKEN") >= 0)
            firebaseBody(original, 2, "return-void")
        }
        for ((name, parameters) in listOf("g" to listOf("Landroid/content/Context;", "Ljava/lang/String;"),
            "h" to listOf("Landroid/content/Context;", "Ljava/lang/String;", "Z"))) {
            editFirebaseMethod("Lg6/i;", name, parameters, "[B") { original -> MutableMethod(original).also(::omitNativeFcmCode) }
        }
        editFirebaseMethod(FIREBASE_SERVICE, "onNewToken", listOf("Ljava/lang/String;"), "V") { original ->
            require(firebaseMethodIndex(original, "Lcom/google/firebase/messaging/FirebaseMessaging;", "subscribeToTopic") >= 0)
            firebaseBody(original, 2, "return-void")
        }
        // 독립적인 기존 측정 queue 전송 방어. IOException은 원래 URL factory의 실패 계약이다.
        editFirebaseMethod("Lcom/google/android/gms/internal/measurement/zzcq;", "zza", listOf("Ljava/net/URL;", "Ljava/lang/String;"), "Ljava/net/URLConnection;", ::firebaseNoConnection)
        editFirebaseMethod("Lcom/google/firebase/crashlytics/internal/network/HttpGetRequest;", "execute", emptyList(), "Lcom/google/firebase/crashlytics/internal/network/HttpResponse;", ::firebaseNoConnection)
        editFirebaseMethod("Lcom/google/firebase/installations/remote/FirebaseInstallationServiceClient;", "openHttpURLConnection", listOf("Ljava/net/URL;", "Ljava/lang/String;"), "Ljava/net/HttpURLConnection;", ::firebaseNoConnection)

    }
}

/** 역할: 현재 변경 가능한 클래스에서 메서드 복사본만 교체한다. 입력: 전체 prototype과 변환기. 출력: 선행 패치를 보존한 클래스. */
private fun BytecodePatchContext.editFirebaseMethod(owner: String, name: String, parameters: List<String>, result: String, transform: (Method) -> MutableMethod) {
    val current = mutableClassDefBy(owner)
    val original = current.methods.single { it.name == name && it.parameterTypes == parameters && it.returnType == result }
    val replacement = transform(original)
    check(current.methods.remove(original))
    current.methods.add(replacement)
}

/** 역할: 예외/디버그 위치가 남지 않는 완전한 메서드 본문을 생성한다. 입력: 원본과 레지스터 수 및 smali. 출력: 동일 prototype의 새 메서드. */
private fun firebaseBody(original: Method, registers: Int, body: String): MutableMethod = MutableMethod(
    ImmutableMethod(original.definingClass, original.name, original.parameters, original.returnType, original.accessFlags,
        original.annotations, original.hiddenApiRestrictions, ImmutableMethodImplementation(registers, emptyList(), emptyList(), emptyList()))
).also { it.addInstructions(0, body) }

/** 역할: 초기 SDK 생성과 구독 구간만 제거한다. 입력: IntroActivity.onCreate 복사본. 출력: 네트워크 확인·알림 채널·딥링크를 보존한 메서드. */
internal fun removeFirebaseStartup(method: MutableMethod) {
    require(method.definingClass == FIREBASE_INTRO && method.name == "onCreate")
    val ins = method.implementation!!.instructions.toList()
    val apps = firebaseMethodIndex(method, "Lcom/google/firebase/FirebaseApp;", "getApps")
    val initialize = firebaseMethodIndex(method, "Lcom/google/firebase/FirebaseApp;", "initializeApp")
    val subscribe = firebaseMethodIndex(method, "Lcom/google/firebase/messaging/FirebaseMessaging;", "getInstance")
    val token = firebaseMethodIndex(method, "Lg6/i;", "f")
    require(initialize == apps + 5 && ins[apps + 4].opcode == Opcode.IF_EQZ &&
        ins[initialize + 1].opcode == Opcode.SGET && subscribe < token &&
        ins[subscribe - 1].opcode == Opcode.IF_NEZ &&
        firebaseStringIndex(method, "riro_noti") == subscribe + 2 &&
        firebaseMethodIndex(method, "Lcom/google/firebase/messaging/FirebaseMessaging;", "subscribeToTopic") == subscribe + 3)
    require(token - subscribe == 8 && ins[token + 1].opcode == Opcode.INVOKE_VIRTUAL)
    // f는 이 패치에서 local-only 토큰 키 제거로 교체한다. 시작할 때 정리하도록 호출은 보존한다.
    method.removeInstructions(subscribe, token - subscribe)
    method.removeInstructions(apps, initialize - apps + 1)
}

/** 역할: 푸시 토큰 실패 재시도 대신 기존 정상 시작 경로로 진행한다. 입력: 원본 Intro.c. 출력: 원본 d 호출 후 반환하는 메서드. */
internal fun firebaseProgression(method: Method): MutableMethod {
    require(method.definingClass == FIREBASE_INTRO && method.name == "c" && method.parameterTypes.isEmpty() && method.returnType == "V")
    firebaseMethodIndex(method, "Lg6/i;", "f")
    firebaseMethodIndex(method, FIREBASE_INTRO, "d")
    return firebaseBody(method, 1, "invoke-virtual {p0}, $FIREBASE_INTRO->d()V\nreturn-void")
}

/** 역할: 저장 토큰을 읽거나 발급하지 않고 정확한 앱 토큰 키만 제거한다. 입력: 원본 g6.i.f. 출력: 명시적 미지원 상태인 빈 문자열. */
internal fun firebaseEmptyToken(method: Method): MutableMethod {
    require(method.definingClass == "Lg6/i;" && method.name == "f" && method.parameterTypes == listOf("Landroid/content/Context;") && method.returnType == "Ljava/lang/String;")
    firebaseMethodIndex(method, "Lcom/google/firebase/messaging/FirebaseMessaging;", "getToken")
    firebaseStringIndex(method, "APP_FCM_TOKEN")
    return firebaseBody(method, 4, """
        const-string v0, ""
        sput-object v0, Lg6/i;->c:Ljava/lang/String;
        const-string v1, "stickyPref"
        const/4 v2, 0x0
        invoke-virtual {p0, v1, v2}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
        move-result-object v1
        invoke-interface {v1}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
        move-result-object v1
        const-string v2, "APP_FCM_TOKEN"
        invoke-interface {v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->remove(Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
        invoke-interface {v1}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
        return-object v0
    """.trimIndent())
}

/** 역할: UUID 처리의 선후와 무관하게 native 폼에서 fcmCode 키·값만 생략한다. 입력: 현재 g/h 폼 생성기. 출력: 다른 필드를 보존한 메서드. */
internal fun omitNativeFcmCode(method: MutableMethod) {
    require(method.definingClass == "Lg6/i;" && method.name in listOf("g", "h") && method.returnType == "[B")
    val ins = method.implementation!!.instructions.toList()
    val key = firebaseStringIndex(method, "fcmCode=")
    val getter = firebaseMethodIndex(method, "Lg6/i;", "f")
    val constructors = ins.indices.filter { firebaseMethodRef(ins[it])?.let { ref ->
        ref.definingClass == "Ljava/lang/StringBuilder;" && ref.name == "<init>" && ref.parameterTypes == listOf("Ljava/lang/String;")
    } == true }
    require(constructors.isNotEmpty())
    val builder = constructors.first()
    require(getter == key + 1 && ins[getter + 1].opcode == Opcode.MOVE_RESULT_OBJECT && builder > getter &&
        firebaseMethodRef(ins[builder + 1])?.let { it.definingClass == "Ljava/net/URLEncoder;" && it.name == "encode" } == true &&
        ins[builder + 2].opcode == Opcode.MOVE_RESULT_OBJECT &&
        firebaseMethodRef(ins[builder + 3])?.let { it.definingClass == "Ljava/lang/StringBuilder;" && it.name == "append" } == true)
    val register = (ins[key] as OneRegisterInstruction).registerA
    method.removeInstructions(builder + 1, 3)
    method.removeInstructions(getter, 2)
    method.replaceInstruction(key, "const-string v$register, \"\"")
}

/** 역할: 전송 이벤트를 직렬화·기기 정보 수집·저장 전에 폐기하고 완료 콜백을 한 번 전달한다. 입력: TransportImpl.a 원본. 출력: 로컬 폐기 승인 콜백만 실행하는 메서드. */
internal fun firebaseTransportIngress(method: Method): MutableMethod {
    require(method.definingClass == "Li3/r;" && method.name == "a" && method.parameterTypes == listOf("Lf3/d;", "Lf3/i;") && method.returnType == "V")
    firebaseMethodIndex(method, "Lf3/f;", "apply")
    firebaseMethodIndex(method, "Ljava/util/concurrent/Executor;", "execute")
    return firebaseBody(method, 4, "const/4 v0, 0x0\ninvoke-interface {p2, v0}, Lf3/i;->b(Ljava/lang/Exception;)V\nreturn-void")
}

/** 역할: 이전 queue의 모든 배치를 기존 unknown-backend 폐기 분기로 보낸다. 입력: 원본 Uploader.a 복사본. 출력: 원래 트랜잭션·삭제·후처리를 사용하는 메서드. */
internal fun dropQueuedTelemetry(method: MutableMethod) {
    require(method.definingClass == "Lo3/i;" && method.name == "a" && method.parameterTypes == listOf("Li3/j;", "I") && method.returnType == "V")
    val ins = method.implementation!!.instructions.toList()
    val backend = firebaseMethodIndex(method, "Lj3/f;", "a")
    firebaseMethodIndex(method, "Ljava/net/URL;", "openConnection")
    firebaseStringIndex(method, "Unknown backend for %s, deleting event batch for it...")
    require(ins[backend + 1].opcode == Opcode.MOVE_RESULT_OBJECT && ins[backend + 2].opcode == Opcode.CONST_WIDE_16)
    val register = (ins[backend + 1] as OneRegisterInstruction).registerA
    method.replaceInstruction(backend, "const/4 v$register, 0x0")
    method.removeInstructions(backend + 1, 1)
}

/** 역할: SDK 전용 URL 진입점을 원래 네트워크 실패 계약으로 차단한다. 입력: URL.openConnection을 정확히 한 번 호출하는 원본. 출력: 연결 전에 IOException을 던지는 메서드. */
internal fun firebaseNoConnection(method: Method): MutableMethod {
    firebaseMethodIndex(method, "Ljava/net/URL;", "openConnection")
    return firebaseBody(method, method.parameterTypes.size + 3, "new-instance v0, Ljava/io/IOException;\nconst-string v1, \"Firebase network disabled by local policy\"\ninvoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V\nthrow v0")
}

/** 역할: manifest의 요소를 변경 전에 열거한다. 입력: DOM 문서. 출력: 실제 Element의 목록. */
internal fun firebaseManifestElements(document: Document): List<Element> {
    val all = document.getElementsByTagName("*")
    return (0 until all.length).map { all.item(it) as Element }
}

/** 역할: 검증된 telemetry 작업 구성요소만 사용 중지한다. 입력: 원본 또는 Firebase가 수정한 DOM. 출력: BLE 등 다른 구성요소를 보존한 문서. */
private fun disableTransportComponents(document: Document) {
    for (name in listOf("com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
        "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver")) {
        val element = firebaseManifestElements(document).single { it.getAttribute("android:name") == name }
        element.setAttributeNS(ANDROID_XML, "android:enabled", "false")
    }
}

/** 역할: 자동 초기화·수집과 명시적 재초기화 registrar를 엄격 프로필로 제한한다. 입력: 승인된 앱의 manifest. 출력: ML Kit·BLE·계정 구성요소를 보존한 문서. */
internal fun applyFirebaseManifest(document: Document) {
    val elements = firebaseManifestElements(document)
    val disabled = listOf("com.google.firebase.provider.FirebaseInitProvider", "com.google.firebase.iid.FirebaseInstanceIdReceiver",
        "com.google.firebase.messaging.FirebaseMessagingService", "com.rirosoft.riroschool.service.MyFirebaseMessagingService",
        "com.google.firebase.sessions.SessionLifecycleService", "com.google.android.gms.measurement.AppMeasurementReceiver",
        "com.google.android.gms.measurement.AppMeasurementService", "com.google.android.gms.measurement.AppMeasurementJobService")
    val targets = disabled.map { name -> elements.single { it.getAttribute("android:name") == name } }
    val registrars = listOf("com.google.firebase.messaging.FirebaseMessagingKtxRegistrar", "com.google.firebase.messaging.FirebaseMessagingRegistrar",
        "com.google.firebase.crashlytics.FirebaseCrashlyticsKtxRegistrar", "com.google.firebase.crashlytics.CrashlyticsRegistrar",
        "com.google.firebase.analytics.connector.internal.AnalyticsConnectorRegistrar", "com.google.firebase.sessions.FirebaseSessionsRegistrar",
        "com.google.firebase.installations.FirebaseInstallationsKtxRegistrar", "com.google.firebase.installations.FirebaseInstallationsRegistrar",
        "com.google.firebase.datatransport.TransportRegistrar").map { name -> elements.single { it.getAttribute("android:name") == "com.google.firebase.components:$name" } }
    val application = elements.single { it.tagName == "application" }
    val flags = mapOf("firebase_data_collection_default_enabled" to "false", "firebase_analytics_collection_enabled" to "false",
        "firebase_analytics_collection_deactivated" to "true", "firebase_crashlytics_collection_enabled" to "false",
        "firebase_sessions_enabled" to "false", "firebase_messaging_auto_init_enabled" to "false",
        "delivery_metrics_exported_to_big_query_enabled" to "false", "google_analytics_adid_collection_enabled" to "false",
        "google_analytics_ssaid_collection_enabled" to "false")
    require(flags.keys.none { key -> elements.any { it.tagName == "meta-data" && it.parentNode == application && it.getAttribute("android:name") == key } })
    targets.forEach { it.setAttributeNS(ANDROID_XML, "android:enabled", "false") }
    registrars.forEach { it.parentNode.removeChild(it) }
    for ((key, value) in flags) {
        val element = document.createElement("meta-data")
        element.setAttributeNS(ANDROID_XML, "android:name", key)
        element.setAttributeNS(ANDROID_XML, "android:value", value)
        application.appendChild(element)
    }
}

/** 역할: 전체 prototype에서 고유한 메서드 참조를 요구한다. 입력: 메서드와 대상 클래스·이름. 출력: 유일한 인덱스 또는 예외. */
private fun firebaseMethodIndex(method: Method, owner: String, name: String): Int {
    val instructions = method.implementation!!.instructions.toList()
    val indices = instructions.indices.filter { index ->
        firebaseMethodRef(instructions[index])?.let { it.definingClass == owner && it.name == name } == true
    }
    require(indices.size == 1) { "Firebase: expected unique $owner->$name in ${method.definingClass}->${method.name}, found ${indices.size}" }
    val expected = when ("$owner->$name") {
        "Landroid/app/job/JobScheduler;->schedule" -> "(Landroid/app/job/JobInfo;)I"
        "Lcom/google/firebase/FirebaseApp;->getApps" -> "(Landroid/content/Context;)Ljava/util/List;"
        "Lcom/google/firebase/FirebaseApp;->initializeApp" -> "(Landroid/content/Context;)Lcom/google/firebase/FirebaseApp;"
        "Lcom/google/firebase/messaging/FirebaseMessaging;->getInstance" -> "()Lcom/google/firebase/messaging/FirebaseMessaging;"
        "Lcom/google/firebase/messaging/FirebaseMessaging;->subscribeToTopic" -> "(Ljava/lang/String;)Lcom/google/android/gms/tasks/Task;"
        "Lcom/google/firebase/messaging/FirebaseMessaging;->getToken" -> "()Lcom/google/android/gms/tasks/Task;"
        "Lg6/i;->f" -> "(Landroid/content/Context;)Ljava/lang/String;"
        "$FIREBASE_INTRO->d" -> "()V"
        "Lf3/f;->apply" -> "(Ljava/lang/Object;)Ljava/lang/Object;"
        "Ljava/util/concurrent/Executor;->execute" -> "(Ljava/lang/Runnable;)V"
        "Lj3/f;->a" -> "(Ljava/lang/String;)Lj3/g;"
        "Ljava/net/URL;->openConnection" -> "()Ljava/net/URLConnection;"
        else -> error("Firebase: no approved anchor prototype for $owner->$name")
    }
    val reference = firebaseMethodRef(instructions[indices.single()])!!
    require("(${reference.parameterTypes.joinToString("")})${reference.returnType}" == expected) { "Firebase: unexpected prototype for $owner->$name" }
    return indices.single()
}

/** 역할: 정확한 문자열 앵커를 찾는다. 입력: 메서드와 문자열. 출력: 유일한 인덱스 또는 예외. */
private fun firebaseStringIndex(method: Method, value: String): Int {
    val instructions = method.implementation!!.instructions.toList()
    val indices = instructions.indices.filter { index ->
        ((instructions[index] as? ReferenceInstruction)?.reference as? StringReference)?.string == value
    }
    require(indices.size == 1) { "Firebase: expected unique string $value in ${method.name}" }
    return indices.single()
}

/** 역할: 명령에서 메서드 참조만 읽는다. 입력: 임의 명령. 출력: 메서드 참조 또는 null. */
private fun firebaseMethodRef(instruction: Instruction): MethodReference? = (instruction as? ReferenceInstruction)?.reference as? MethodReference

@Suppress("unused")
val disableFirebaseAnalyticsPatch = resourcePatch(
    name = "Disable Firebase analytics",
    description = "Disable Firebase telemetry and token registration. Push is unavailable; also disables shared app DataTransport telemetry.",
    default = true,
) {
    compatibleWith(riroCompatibility)
    dependsOn(firebaseAnalyticsBytecodePatch)
    execute {
        requireRiroTarget(packageMetadata)
        document("AndroidManifest.xml").use(::applyFirebaseManifest)
    }
}