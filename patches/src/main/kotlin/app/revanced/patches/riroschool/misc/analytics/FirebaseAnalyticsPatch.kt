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
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
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
        val ingress = firebaseFingerprint("transport ingress", ::firebaseIsTransportIngress)
        val uploader = firebaseFingerprint("queued uploader", ::firebaseIsUploader)
        val scheduler = firebaseFingerprint("job scheduler", ::firebaseIsScheduler)
        val callback = classDefBy(ingress.parameterTypes[1].toString()).methods.single {
            it.parameterTypes == listOf("Ljava/lang/Exception;") && it.returnType == "V"
        }
        val replacements = listOf(
            ingress to firebaseTransportIngress(ingress, callback),
            uploader to MutableMethod(uploader).also(::dropQueuedTelemetry),
            scheduler to firebaseBody(scheduler, 4, "return-void"),
        )
        replacements.forEach { (original, replacement) -> replaceFirebaseMethod(original, replacement) }

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
        val startup = firebaseFingerprint("app initialization") {
            it.definingClass == FIREBASE_INTRO && it.name == "onCreate" &&
                it.parameterTypes == listOf("Landroid/os/Bundle;") &&
                firebaseCalls(it, "Lcom/google/firebase/FirebaseApp;", "initializeApp", "(Landroid/content/Context;)Lcom/google/firebase/FirebaseApp;")
        }
        val progression = firebaseFingerprint("token progression") {
            it.definingClass == FIREBASE_INTRO && it.parameterTypes.isEmpty() && it.returnType == "V" &&
                firebaseCalls(it, "Lcom/google/firebase/messaging/FirebaseMessaging;", "getToken", "()Lcom/google/android/gms/tasks/Task;")
        }
        val token = firebaseFingerprint("app token getter", ::firebaseIsTokenGetter)
        val writer = firebaseFingerprint("app token persistence") {
            it.parameterTypes == listOf("Landroid/content/Context;", "Ljava/lang/String;") && it.returnType == "V" &&
                firebaseHasString(it, "APP_FCM_TOKEN") &&
                firebaseCalls(it, "Landroid/content/SharedPreferences\$Editor;", "putString", "(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences\$Editor;")
        }
        val forms = listOf(
            listOf("Landroid/content/Context;", "Ljava/lang/String;"),
            listOf("Landroid/content/Context;", "Ljava/lang/String;", "Z"),
        ).map { parameters -> firebaseFingerprint("native FCM form $parameters") {
            it.parameterTypes == parameters && it.returnType == "[B" && firebaseHasString(it, "fcmCode=") &&
                firebaseHasString(it, "&app_version=") && firebaseHasString(it, "&cookie_token=")
        } }
        val newToken = firebaseFingerprint("token refresh") {
            it.definingClass == FIREBASE_SERVICE && it.name == "onNewToken" &&
                it.parameterTypes == listOf("Ljava/lang/String;") &&
                firebaseCalls(it, "Lcom/google/firebase/messaging/FirebaseMessaging;", "subscribeToTopic", "(Ljava/lang/String;)Lcom/google/android/gms/tasks/Task;")
        }
        val network = listOf(
            firebaseFingerprint("measurement connection") {
                it.definingClass.startsWith("Lcom/google/android/gms/internal/measurement/") &&
                    it.parameterTypes == listOf("Ljava/net/URL;", "Ljava/lang/String;") &&
                    it.returnType == "Ljava/net/URLConnection;" && firebaseCalls(it, "Ljava/net/URL;", "openConnection", "()Ljava/net/URLConnection;")
            },
            firebaseFingerprint("Crashlytics settings connection") {
                it.definingClass == "Lcom/google/firebase/crashlytics/internal/network/HttpGetRequest;" && it.name == "execute" &&
                    it.parameterTypes.isEmpty() && firebaseCalls(it, "Ljava/net/URL;", "openConnection", "()Ljava/net/URLConnection;")
            },
            firebaseFingerprint("installation connection") {
                it.definingClass == "Lcom/google/firebase/installations/remote/FirebaseInstallationServiceClient;" &&
                    it.name == "openHttpURLConnection" && firebaseCalls(it, "Ljava/net/URL;", "openConnection", "()Ljava/net/URLConnection;")
            },
        )
        // Resolve and validate every replacement before modifying this patch's input methods.
        val replacements = mutableListOf(
            startup to MutableMethod(startup).also(::removeFirebaseStartup),
            progression to firebaseProgression(progression),
            token to firebaseEmptyToken(token),
            writer to firebaseBody(writer, 2, "return-void"),
            newToken to firebaseBody(newToken, 2, "return-void"),
        )
        replacements += forms.map { it to MutableMethod(it).also(::omitNativeFcmCode) }
        replacements += network.map { it to firebaseNoConnection(it) }
        replacements.forEach { (original, replacement) -> replaceFirebaseMethod(original, replacement) }

    }
}

/** Replace only a validated method copy while preserving preceding patches. */
private fun BytecodePatchContext.replaceFirebaseMethod(original: Method, replacement: MutableMethod) {
    val current = mutableClassDefBy(original.definingClass)
    val existing = current.methods.single {
        it.name == original.name && it.parameterTypes == original.parameterTypes && it.returnType == original.returnType
    }
    check(current.methods.remove(existing))
    current.methods.add(replacement)
}

/** Build a replacement body with no stale exception or debug positions. */
private fun firebaseBody(original: Method, registers: Int, body: String): MutableMethod = MutableMethod(
    ImmutableMethod(original.definingClass, original.name, original.parameters, original.returnType, original.accessFlags,
        original.annotations, original.hiddenApiRestrictions, ImmutableMethodImplementation(registers, emptyList(), emptyList(), emptyList()))
).also { it.addInstructions(0, body) }

/** Remove SDK initialization and subscription while retaining startup and notification handling. */
internal fun removeFirebaseStartup(method: MutableMethod) {
    require(method.definingClass == FIREBASE_INTRO && method.name == "onCreate")
    val ins = method.implementation!!.instructions.toList()
    val apps = firebaseMethodIndex(method, "Lcom/google/firebase/FirebaseApp;", "getApps")
    val initialize = firebaseMethodIndex(method, "Lcom/google/firebase/FirebaseApp;", "initializeApp")
    val subscribe = firebaseMethodIndex(method, "Lcom/google/firebase/messaging/FirebaseMessaging;", "getInstance")
    val token = subscribe + 8
    val tokenReference = firebaseMethodRef(ins[token])
    require(tokenReference?.parameterTypes == listOf("Landroid/content/Context;") && tokenReference.returnType == "Ljava/lang/String;")
    require(initialize == apps + 5 && ins[apps + 4].opcode == Opcode.IF_EQZ &&
        ins[initialize + 1].opcode == Opcode.SGET && subscribe < token &&
        ins[subscribe - 1].opcode == Opcode.IF_NEZ &&
        firebaseStringIndex(method, "riro_noti") == subscribe + 2 &&
        firebaseMethodIndex(method, "Lcom/google/firebase/messaging/FirebaseMessaging;", "subscribeToTopic") == subscribe + 3)
    require(token - subscribe == 8 && ins[token + 1].opcode == Opcode.INVOKE_VIRTUAL)
    // Retain the token getter call so the patched local-only cleanup still runs at startup.
    method.removeInstructions(subscribe, token - subscribe)
    method.removeInstructions(apps, initialize - apps + 1)
}

/** Continue through the original success path instead of retrying unavailable push tokens. */
internal fun firebaseProgression(method: Method): MutableMethod {
    require(method.definingClass == FIREBASE_INTRO && method.parameterTypes.isEmpty() && method.returnType == "V")
    val continuation = method.implementation!!.instructions.mapNotNull(::firebaseMethodRef).single {
        it.definingClass == method.definingClass && it.parameterTypes.isEmpty() && it.returnType == "V"
    }
    require(firebaseCalls(method, "Lcom/google/firebase/messaging/FirebaseMessaging;", "getToken", "()Lcom/google/android/gms/tasks/Task;"))
    return firebaseBody(method, 1, "invoke-virtual {p0}, $continuation\nreturn-void")
}

/** Clear only the persisted app token and return an explicit unsupported empty token. */
internal fun firebaseEmptyToken(method: Method): MutableMethod {
    require(firebaseIsTokenGetter(method))
    firebaseMethodIndex(method, "Lcom/google/firebase/messaging/FirebaseMessaging;", "getToken")
    firebaseStringIndex(method, "APP_FCM_TOKEN")
    val field = method.implementation!!.instructions.filter { it.opcode == Opcode.SGET_OBJECT }
        .mapNotNull { (it as? ReferenceInstruction)?.reference as? FieldReference }
        .filter { it.definingClass == method.definingClass && it.type == "Ljava/lang/String;" }
        .distinctBy { it.toString() }.single()
    return firebaseBody(method, 4, """
        const-string v0, ""
        sput-object v0, $field
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

/** Omit only the native fcmCode field, independently of device_id filtering order. */
internal fun omitNativeFcmCode(method: MutableMethod) {
    require(method.returnType == "[B" && method.parameterTypes.firstOrNull() == "Landroid/content/Context;")
    val ins = method.implementation!!.instructions.toList()
    val key = firebaseStringIndex(method, "fcmCode=")
    val getter = key + 1
    require(firebaseMethodRef(ins[getter])?.let {
        it.parameterTypes == listOf("Landroid/content/Context;") && it.returnType == "Ljava/lang/String;"
    } == true)
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

/** Discard telemetry before serialization and acknowledge completion once. */
internal fun firebaseTransportIngress(method: Method, callback: MethodReference): MutableMethod {
    require(firebaseIsTransportIngress(method))
    require(callback.definingClass == method.parameterTypes[1].toString() && callback.parameterTypes.map { it.toString() } == listOf("Ljava/lang/Exception;") && callback.returnType == "V")
    firebaseMethodIndex(method, "Ljava/util/concurrent/Executor;", "execute")
    return firebaseBody(method, 4, "const/4 v0, 0x0\ninvoke-interface {p2, v0}, ${callback}\nreturn-void")
}

/** Route queued batches through the existing unknown-backend discard transaction. */
internal fun dropQueuedTelemetry(method: MutableMethod) {
    require(firebaseIsUploader(method))
    val ins = method.implementation!!.instructions.toList()
    val backend = firebaseBackendIndex(method)
    firebaseMethodIndex(method, "Ljava/net/URL;", "openConnection")
    firebaseStringIndex(method, "Unknown backend for %s, deleting event batch for it...")
    require(ins[backend + 1].opcode == Opcode.MOVE_RESULT_OBJECT && ins[backend + 2].opcode == Opcode.CONST_WIDE_16)
    val register = (ins[backend + 1] as OneRegisterInstruction).registerA
    method.replaceInstruction(backend, "const/4 v$register, 0x0")
    method.removeInstructions(backend + 1, 1)
}

/** Fail SDK-specific URL entry points through their existing IOException contract. */
internal fun firebaseNoConnection(method: Method): MutableMethod {
    firebaseMethodIndex(method, "Ljava/net/URL;", "openConnection")
    return firebaseBody(method, method.parameterTypes.size + 3, "new-instance v0, Ljava/io/IOException;\nconst-string v1, \"Firebase network disabled by local policy\"\ninvoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V\nthrow v0")
}

/** Enumerate manifest elements before applying changes. */
internal fun firebaseManifestElements(document: Document): List<Element> {
    val all = document.getElementsByTagName("*")
    return (0 until all.length).map { all.item(it) as Element }
}

/** Disable only the verified telemetry scheduling components. */
private fun disableTransportComponents(document: Document) {
    for (name in listOf("com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
        "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver")) {
        val element = firebaseManifestElements(document).single { it.getAttribute("android:name") == name }
        element.setAttributeNS(ANDROID_XML, "android:enabled", "false")
    }
}

/** Apply strict Firebase collection and registration policy while retaining unrelated components. */
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

/** Require a unique public API call and verify its complete prototype. */
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
        "Ljava/util/concurrent/Executor;->execute" -> "(Ljava/lang/Runnable;)V"
        "Ljava/net/URL;->openConnection" -> "()Ljava/net/URLConnection;"
        else -> error("Firebase: no approved anchor prototype for $owner->$name")
    }
    val reference = firebaseMethodRef(instructions[indices.single()])!!
    require("(${reference.parameterTypes.joinToString("")})${reference.returnType}" == expected) { "Firebase: unexpected prototype for $owner->$name" }
    return indices.single()
}

/** Require one exact stable string anchor. */
private fun firebaseStringIndex(method: Method, value: String): Int {
    val instructions = method.implementation!!.instructions.toList()
    val indices = instructions.indices.filter { index ->
        ((instructions[index] as? ReferenceInstruction)?.reference as? StringReference)?.string == value
    }
    require(indices.size == 1) { "Firebase: expected unique string $value in ${method.name}" }
    return indices.single()
}

/** Read a method reference safely from an arbitrary DEX instruction. */
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