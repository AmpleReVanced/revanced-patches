package app.revanced.patches.riroschool

import app.revanced.patches.riroschool.misc.analytics.*
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import java.io.File

/** Exercise the production predicates and uniqueness gate against the original and renamed fixtures. */
fun runFirebaseFingerprintTests() {
    val dex = DexFileFactory.loadDexFile(File(System.getenv("RIRO_TEST_APK") ?: error("Set RIRO_TEST_APK")), Opcodes.getDefault())
    val methods = dex.classes.flatMap { it.methods }
    val predicates = listOf(::firebaseIsTransportIngress, ::firebaseIsUploader, ::firebaseIsScheduler, ::firebaseIsTokenGetter)
    val sdkBoundaries = listOf(
        "Ljava/util/concurrent/Executor;" to "execute",
        "Ljava/net/URL;" to "openConnection",
        "Landroid/app/job/JobScheduler;" to "schedule",
        "Lcom/google/firebase/messaging/FirebaseMessaging;" to "getToken",
    )
    for ((index, predicate) in predicates.withIndex()) {
        val method = firebaseSelectUnique(methods, "original $index", predicate)
        val renamed = ImmutableMethod("Lrenamed/Target$index;", "renamed$index", method.parameters,
            method.returnType, method.accessFlags, method.annotations, method.hiddenApiRestrictions, method.implementation)
        check(predicate(renamed)) { "Fingerprint depends on the target class or method name: $index" }
        check(firebaseSelectUnique(listOf(renamed), "renamed $index", predicate) === renamed)
        check(runCatching { firebaseSelectUnique(emptyList(), "missing $index", predicate) }.isFailure)
        check(runCatching { firebaseSelectUnique(listOf(method, renamed), "ambiguous $index", predicate) }.isFailure)
        val missingCode = ImmutableMethod(renamed.definingClass, renamed.name, renamed.parameters,
            renamed.returnType, renamed.accessFlags, renamed.annotations, renamed.hiddenApiRestrictions, null)
        check(!predicate(missingCode))
        val malformedSdkCall = MutableMethod(method)
        val (sdkOwner, sdkName) = sdkBoundaries[index]
        val callIndex = malformedSdkCall.implementation!!.instructions.indices.single { instructionIndex ->
            ((malformedSdkCall.implementation!!.instructions[instructionIndex] as? ReferenceInstruction)?.reference as? MethodReference)
                ?.let { it.definingClass == sdkOwner && it.name == sdkName } == true
        }
        val call = malformedSdkCall.implementation!!.instructions[callIndex] as FiveRegisterInstruction
        val reference = (call as ReferenceInstruction).reference as MethodReference
        malformedSdkCall.replaceInstruction(callIndex, BuilderInstruction35c(
            malformedSdkCall.implementation!!.instructions[callIndex].opcode,
            call.registerCount, call.registerC, call.registerD, call.registerE, call.registerF, call.registerG,
            ImmutableMethodReference(reference.definingClass, reference.name, reference.parameterTypes, "J"),
        ))
        check(!predicate(malformedSdkCall)) { "Malformed SDK prototype was accepted: $sdkOwner->$sdkName" }
    }
    val ingress = firebaseSelectUnique(methods, "ingress", ::firebaseIsTransportIngress)
    val renamedIngress = ImmutableMethod("Lrenamed/Ingress;", "dispatch", ingress.parameters,
        ingress.returnType, ingress.accessFlags, ingress.annotations, ingress.hiddenApiRestrictions, ingress.implementation)
    val callback = ImmutableMethodReference(ingress.parameterTypes[1].toString(), "renamedCallback", listOf("Ljava/lang/Exception;"), "V")
    val dropped = firebaseTransportIngress(renamedIngress, callback)
    val invoked = dropped.implementation!!.instructions.mapNotNull { (it as? ReferenceInstruction)?.reference as? MethodReference }.single()
    check(invoked.toString() == callback.toString())
    check(runCatching { firebaseTransportIngress(renamedIngress,
        ImmutableMethodReference("Lwrong/Callback;", "wrong", listOf("Ljava/lang/Exception;"), "V")) }.isFailure)

    val getter = firebaseSelectUnique(methods, "getter", ::firebaseIsTokenGetter)
    val renamedGetter = MutableMethod(ImmutableMethod("Lrenamed/Tokens;", "load", getter.parameters,
        getter.returnType, getter.accessFlags, getter.annotations, getter.hiddenApiRestrictions, getter.implementation))
    renamedGetter.implementation!!.instructions.toList().forEachIndexed { index, instruction ->
        if (instruction.opcode == Opcode.SGET_OBJECT) {
            renamedGetter.replaceInstruction(index,
                "sget-object v${(instruction as OneRegisterInstruction).registerA}, Lrenamed/Tokens;->cached:Ljava/lang/String;")
        }
    }
    val empty = firebaseEmptyToken(renamedGetter)
    check(empty.implementation!!.instructions.any {
        ((it as? ReferenceInstruction)?.reference)?.toString() == "Lrenamed/Tokens;->cached:Ljava/lang/String;"
    })
    println("PASS: Firebase fingerprints reject missing/ambiguous inputs and preserve renamed targets/references")
}

internal object FirebaseFingerprintTestRunner {
    /** Run the standalone fingerprint fixtures without requiring a patch session. */
    @JvmStatic fun main(args: Array<String>) = runFirebaseFingerprintTests()
}
