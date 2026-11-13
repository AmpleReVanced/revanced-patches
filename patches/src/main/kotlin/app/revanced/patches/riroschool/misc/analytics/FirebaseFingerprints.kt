package app.revanced.patches.riroschool.misc.analytics

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/** Resolve one semantic match in the current APK without retaining cross-session state. */
internal fun BytecodePatchContext.firebaseFingerprint(label: String, predicate: (Method) -> Boolean): Method {
    val matches = Fingerprint(custom = { method, _ -> predicate(method) }).matchAll()
    // Read the current method so earlier selected patches are not overwritten by an original snapshot.
    return firebaseSelectUnique(matches.map { it.method }, label) { true }
}

/** Reject missing and ambiguous semantic targets instead of taking the first match. */
internal fun firebaseSelectUnique(methods: List<Method>, label: String, predicate: (Method) -> Boolean): Method {
    val matches = methods.filter(predicate)
    require(matches.size == 1) { "Firebase $label: expected one match, found ${matches.size}" }
    return matches.single()
}

/** Return whether the method contains the exact stable string anchor. */
internal fun firebaseHasString(method: Method, text: String): Boolean =
    method.implementation?.instructions?.any {
        ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == text
    } == true

/** Match the exact SDK owner, name and prototype without assuming the containing method identity. */
internal fun firebaseCalls(method: Method, owner: String, name: String, descriptor: String): Boolean =
    method.implementation?.instructions?.mapNotNull {
        (it as? ReferenceInstruction)?.reference as? MethodReference
    }?.filter { it.definingClass == owner && it.name == name }?.singleOrNull()?.let { ref ->
        "(${ref.parameterTypes.joinToString("")})${ref.returnType}" == descriptor
    } == true

/** Identify transport ingress by null-contract anchors and executor dispatch. */
internal fun firebaseIsTransportIngress(method: Method): Boolean =
    method.returnType == "V" && method.parameterTypes.size == 2 &&
        method.parameterTypes.all { it.startsWith("L") } &&
        listOf("Null encoding", "Null transformer", "Null transportName", "Null event", "Null transportContext")
            .all { firebaseHasString(method, it) } &&
        firebaseCalls(method, "Ljava/util/concurrent/Executor;", "execute", "(Ljava/lang/Runnable;)V")

/** Identify the queued uploader by its backend discard contract and URL operation. */
internal fun firebaseIsUploader(method: Method): Boolean =
    method.returnType == "V" && method.parameterTypes.size == 2 &&
        method.parameterTypes[0].startsWith("L") && method.parameterTypes[1] == "I" &&
        firebaseHasString(method, "Unknown backend for %s, deleting event batch for it...") &&
        firebaseCalls(method, "Ljava/net/URL;", "openConnection", "()Ljava/net/URLConnection;")

/** Identify the scheduler overload by platform API and the force-scheduling prototype. */
internal fun firebaseIsScheduler(method: Method): Boolean =
    method.returnType == "V" && method.parameterTypes.size == 3 &&
        method.parameterTypes[0].startsWith("L") && method.parameterTypes.drop(1) == listOf("I", "Z") &&
        firebaseCalls(method, "Landroid/app/job/JobScheduler;", "schedule", "(Landroid/app/job/JobInfo;)I")

/** Identify the app token getter using the persisted key and stable Messaging API. */
internal fun firebaseIsTokenGetter(method: Method): Boolean =
    method.parameterTypes == listOf("Landroid/content/Context;") && method.returnType == "Ljava/lang/String;" &&
        firebaseHasString(method, "APP_FCM_TOKEN") &&
        firebaseCalls(method, "Lcom/google/firebase/messaging/FirebaseMessaging;", "getToken", "()Lcom/google/android/gms/tasks/Task;")

/** Select a unique reference by prototype and neighboring instructions, independent of obfuscation. */
internal fun firebaseBackendIndex(method: Method): Int {
    val instructions = requireNotNull(method.implementation).instructions.toList()
    val matches = instructions.indices.filter { index ->
        val reference = (instructions[index] as? ReferenceInstruction)?.reference as? MethodReference
        reference != null && reference.parameterTypes == listOf("Ljava/lang/String;") &&
            reference.returnType.startsWith("L") && index + 2 < instructions.size &&
            instructions[index + 1].opcode == Opcode.MOVE_RESULT_OBJECT &&
            instructions[index + 2].opcode == Opcode.CONST_WIDE_16
    }
    require(matches.size == 1) { "Firebase uploader: ambiguous backend lookup" }
    return matches.single()
}
