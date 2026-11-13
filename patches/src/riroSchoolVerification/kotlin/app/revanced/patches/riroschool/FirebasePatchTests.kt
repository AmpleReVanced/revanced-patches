package app.revanced.patches.riroschool

import app.revanced.patches.riroschool.misc.analytics.*
import app.revanced.patches.riroschool.misc.integrity.*
import app.revanced.patches.riroschool.shared.*

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal object FirebasePatchTestRunner {
    /** Validate the original artifact contract and reject unsafe transformation anchors. */
    @JvmStatic fun main(args: Array<String>) { runFirebasePatchTests(); println("PASS: Firebase contracts") }
}

/** Validate the original artifact contract and reject unsafe transformation anchors. */
fun runFirebasePatchTests() {
    val dex = DexFileFactory.loadDexFile(File(System.getenv("RIRO_TEST_APK") ?: error("Set RIRO_TEST_APK to the original Riro School 7.5.6 APK")), Opcodes.getDefault())
    val classes = dex.classes.associateBy { it.type }
    /** Validate the original artifact contract and reject unsafe transformation anchors. */
    fun method(owner: String, name: String): Method = classes.getValue(owner).methods.single { it.name == name }
    val intro = "Lcom/rirosoft/riroschool/views/activity/IntroActivity;"
    val firebaseApiOwners = listOf("Lcom/google/firebase/FirebaseApp;", "Lcom/google/firebase/messaging/FirebaseMessaging;",
        "Lcom/google/firebase/analytics/FirebaseAnalytics;", "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;",
        "Lcom/google/firebase/installations/FirebaseInstallations;")
    val appSdkCallers = classes.values.filter { it.type.startsWith("Lcom/rirosoft/") || it.type.startsWith("Lg6/") }.flatMap { owner ->
        owner.methods.filter { it.implementation != null && firebaseRefs(it).any { ref -> firebaseApiOwners.any { ref.startsWith("$it->") } } }
            .map { "${it.definingClass}->${it.name}" }
    }.toSet()
    check(appSdkCallers == setOf("$intro->onCreate", "$intro->c", "Lg6/i;->f", "Lcom/rirosoft/riroschool/service/MyFirebaseMessagingService;->onNewToken")) {
        "Firebase: app SDK caller closure changed: $appSdkCallers"
    }
    val negative = System.getenv("RIRO_FIREBASE_NEGATIVE_CONTROL") == "noop"
    val startup = MutableMethod(method(intro, "onCreate"))
    if (!negative) removeFirebaseStartup(startup)
    check(firebaseRefs(startup).none { "Lcom/google/firebase/FirebaseApp;" in it || "Lcom/google/firebase/messaging/FirebaseMessaging;" in it })
    check(firebaseRefs(startup).count { "Ld6/b;->a" in it } == 1)
    check(firebaseRefs(startup).any { "Lcom/google/firebase/messaging/a;->y" in it })
    check(firebaseRefs(startup).count { "Lg6/i;->f(" in it } == 1) // local token cleanup remains at startup
    checkFirebaseControlFlow(startup)
    val progression = firebaseProgression(method(intro, "c"))
    check(firebaseRefs(progression) == listOf("$intro->d()V"))
    val token = firebaseEmptyToken(method("Lg6/i;", "f"))
    check(firebaseRefs(token).none { "Firebase" in it || "getString" in it })
    check(firebaseRefs(token).contains("APP_FCM_TOKEN")) // exact local key removal; no pref file clear
    check(firebaseRefs(token).any { "->remove(" in it })
    check(firebaseRefs(token).none { "->clear(" in it })
    for (name in listOf("g", "h")) {
        val original = method("Lg6/i;", name)
        val first = MutableMethod(original)
        omitNativeFcmCode(first)
        check("fcmCode=" !in firebaseRefs(first))
        check(firebaseRefs(first).none { "Lg6/i;->f(" in it })
        check("&device_id=" in firebaseRefs(first))
        check("&app_version=" in firebaseRefs(first))
        check(firebaseRefs(first).containsAll(listOf("&site_id=", "&cookie_token=")))
        omitNativeDeviceId(first)
        val second = MutableMethod(original)
        omitNativeDeviceId(second)
        omitNativeFcmCode(second)
        check(firebaseRefs(first) == firebaseRefs(second)) { "fcm/device_id order changed output" }
        checkFirebaseControlFlow(first)
        check(runCatching { omitNativeFcmCode(first) }.isFailure)
    }
    val ingress = firebaseTransportIngress(method("Li3/r;", "a"), classes.getValue("Lf3/i;").methods.single { it.parameterTypes == listOf("Ljava/lang/Exception;") && it.returnType == "V" })
    check(firebaseRefs(ingress) == listOf("Lf3/i;->b(Ljava/lang/Exception;)V"))
    val uploader = MutableMethod(method("Lo3/i;", "a"))
    dropQueuedTelemetry(uploader)
    check(firebaseRefs(uploader).none { "Lj3/f;->a(" in it })
    check(firebaseRefs(uploader).any { "Unknown backend" in it })
    check(firebaseRefs(uploader).any { "Lp3/j;->l(" in it })
    check(runCatching { dropQueuedTelemetry(uploader) }.isFailure)
    checkFirebaseControlFlow(uploader)
    for ((owner, name) in listOf("Lcom/google/android/gms/internal/measurement/zzcq;" to "zza",
        "Lcom/google/firebase/crashlytics/internal/network/HttpGetRequest;" to "execute",
        "Lcom/google/firebase/installations/remote/FirebaseInstallationServiceClient;" to "openHttpURLConnection")) {
        val blocked = firebaseNoConnection(method(owner, name))
        check(blocked.implementation!!.instructions.last().opcode == Opcode.THROW)
        check(firebaseRefs(blocked).none { "->openConnection(" in it || "->getId(" in it })
        check(firebaseRefs(blocked).any { "Ljava/io/IOException;-><init>" in it })
    }
    val malformed = MutableMethod(method(intro, "onCreate"))
    val topic = malformed.implementation!!.instructions.indexOfFirst { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "riro_noti" }
    malformed.replaceInstruction(topic, "const-string v4, \"unexpected\"")
    val malformedBefore = firebaseRefs(malformed)
    check(runCatching { removeFirebaseStartup(malformed) }.isFailure)
    check(firebaseRefs(malformed) == malformedBefore)
    check(runCatching { removeFirebaseStartup(startup) }.isFailure)
    val parser = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
    val manifest = parser.newDocumentBuilder().parse(File(System.getenv("RIRO_TEST_MANIFEST") ?: error("Set RIRO_TEST_MANIFEST to the original decoded manifest")))
    val beforeBle = firebaseManifestElements(manifest).filter { it.getAttribute("android:name").contains("Beacon") }.map { it.cloneNode(true) }
    applyFirebaseManifest(manifest)
    val afterBle = firebaseManifestElements(manifest).filter { it.getAttribute("android:name").contains("Beacon") }
    check(beforeBle.zip(afterBle).all { (a, b) -> a.isEqualNode(b) })
    check(firebaseManifestElements(manifest).single { it.getAttribute("android:name") == "com.google.mlkit.common.internal.MlKitInitProvider" }.getAttribute("android:enabled") != "false")
    check(firebaseManifestElements(manifest).filter { it.getAttribute("android:name").startsWith("com.google.firebase.components:") }.all {
        it.getAttribute("android:name").contains("mlkit") || it.getAttribute("android:name").contains("Common")
    })
    check(runCatching { applyFirebaseManifest(manifest) }.isFailure)
}

/** Validate the original artifact contract and reject unsafe transformation anchors. */
private fun firebaseRefs(method: Method): List<String> = method.implementation!!.instructions.mapNotNull {
    when (val ref = (it as? ReferenceInstruction)?.reference) {
        is MethodReference -> "${ref.definingClass}->${ref.name}(${ref.parameterTypes.joinToString("")})${ref.returnType}"
        is StringReference -> ref.string
        else -> null
    }
}

/** Validate the original artifact contract and reject unsafe transformation anchors. */
private fun checkFirebaseControlFlow(method: Method) {
    val implementation = method.implementation!!
    var total = 0
    val locations = implementation.instructions.associateBy { instruction -> total.also { total += instruction.codeUnits } }
    for ((address, instruction) in locations) if (instruction is OffsetInstruction) check(address + instruction.codeOffset in locations)
    for (block in implementation.tryBlocks) {
        check(block.startCodeAddress in locations && block.codeUnitCount > 0)
        check(block.startCodeAddress + block.codeUnitCount in locations || block.startCodeAddress + block.codeUnitCount == total)
        check(block.exceptionHandlers.all { it.handlerCodeAddress in locations })
    }
}
