package app.revanced.patches.riroschool.misc.analytics

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

/** References captured from an unmodified logger, never from an obfuscated name table. */
internal data class MlKitLogger(
    val owner: String,
    val callable: String,
    val idTask: FieldReference,
    val submitMethod: MethodReference,
    val idGetter: MethodReference,
    val voidNames: Set<String>,
    val metadataMethod: MethodReference?,
)

/** Resolves the preference-backed installation getter and follows its actual callers to three loggers. */
internal fun resolveMlKitLoggers(classes: Collection<ClassDef>): List<MlKitLogger> {
    val methods = classes.flatMap { it.methods.toList() }
    val getter = methods.single { method ->
        method.parameterTypes.isEmpty() && method.returnType == "Ljava/lang/String;" &&
            googleStrings(method).containsAll(listOf("com.google.mlkit.internal", "ml_sdk_instance_id")) &&
            googleCalls(method).any { it.definingClass == "Ljava/util/UUID;" && it.name == "randomUUID" } &&
            googleCalls(method).any { it.definingClass == "Landroid/content/SharedPreferences;" && it.name == "getString" }
    }
    val callables = classes.filter { owner ->
        "Ljava/util/concurrent/Callable;" in owner.interfaces && owner.methods.any { method ->
            method.name == "call" && method.parameterTypes.isEmpty() && googleCalls(method).any { googleSameMethod(it, getter) }
        }
    }
    require(callables.size == 3) { "ML Kit: expected three installation-ID Callables, found ${callables.size}" }
    val resolved = callables.map { callable ->
        val constructor = methods.single { method ->
            method.name == "<init>" && method.parameterTypes.size == 4 &&
                method.parameterTypes.first().toString() == "Landroid/content/Context;" &&
                method.parameterTypes.last().toString() == "Ljava/lang/String;" &&
                method.implementation?.instructions?.any {
                    it.opcode == Opcode.NEW_INSTANCE && (googleReference(it) as? TypeReference)?.type == callable.type
                } == true
        }
        val owner = classes.single { it.type == constructor.definingClass }
        val ins = constructor.implementation!!.instructions.toList()
        val allocation = ins.indices.single { ins[it].opcode == Opcode.NEW_INSTANCE && (googleReference(ins[it]) as? TypeReference)?.type == callable.type }
        val submit = googleReference(ins[allocation + 3]) as? MethodReference
            ?: error("ML Kit: Callable submission reference missing")
        require(submit.parameterTypes.map(CharSequence::toString) == listOf("Ljava/util/concurrent/Callable;") &&
            submit.returnType == "Lcom/google/android/gms/tasks/Task;" &&
            googleCalls(constructor).count { googleSameMethod(it, submit) } == 2) { "ML Kit: unexpected submission helper" }
        val field = googleReference(ins[allocation + 5]) as? FieldReference
            ?: error("ML Kit: installation task field missing")
        require(ins[allocation + 5].opcode == Opcode.IPUT_OBJECT && field.definingClass == owner.type && field.type == submit.returnType)
        val voids = owner.methods.filter { it.returnType == "V" && !it.name.startsWith("<") && it.implementation != null }
        val metadata = owner.methods.filter { it.returnType != "V" && googleCalls(it).any { ref -> googleSameMethod(ref, getter) } }
        require(metadata.size <= 1) { "ML Kit: ambiguous metadata builder" }
        require(voids.size in setOf(2, 6) && voids.map { it.name }.toSet().size == voids.size) { "ML Kit: diagnostic ingress shape changed" }
        MlKitLogger(owner.type, callable.type, field, submit, getter, voids.map { it.name }.toSet(), metadata.singleOrNull())
    }
    require(resolved.map { it.owner }.toSet().size == 3 && resolved.count { it.metadataMethod != null } == 1 &&
        resolved.map { it.voidNames.size }.sorted() == listOf(2, 2, 6)) { "ML Kit: logger inventory changed" }
    return resolved.sortedBy { it.owner }
}

/** Finds each default Firelog option factory from its stable library identity and builder API shape. */
internal fun resolveMlKitFirelogOptions(classes: Collection<ClassDef>): List<Method> =
    listOf("play-services-code-scanner", "common", "vision-common").map { library ->
        classes.flatMap { it.methods.toList() }.single { method ->
            method.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;") && method.returnType.startsWith("L") &&
                library in googleStrings(method) && googleCalls(method).count { it.parameterTypes.map(CharSequence::toString) == listOf("Z") } == 1 &&
                googleCalls(method).count { it.parameterTypes.map(CharSequence::toString) == listOf("I") } == 1 &&
                method.implementation!!.instructions.count { it.opcode == Opcode.NEW_INSTANCE } == 1
        }
    }

/** Reads references without assuming a particular obfuscated owner or member name. */
internal fun googleReference(instruction: Instruction) = (instruction as? ReferenceInstruction)?.reference

/** Returns all method references in execution order. */
internal fun googleCalls(method: Method): List<MethodReference> =
    method.implementation?.instructions?.mapNotNull { googleReference(it) as? MethodReference } ?: emptyList()

/** Returns literal strings used as semantic anchors. */
private fun googleStrings(method: Method): Set<String> =
    method.implementation?.instructions?.mapNotNull { (googleReference(it) as? StringReference)?.string }?.toSet() ?: emptySet()

/** Compares complete prototypes, including CharSequence-backed DEX parameter types. */
internal fun googleSameMethod(left: MethodReference, right: MethodReference): Boolean =
    left.definingClass == right.definingClass && left.name == right.name && left.returnType == right.returnType &&
        left.parameterTypes.map(CharSequence::toString) == right.parameterTypes.map(CharSequence::toString)

/** Compares a resolved field by owner, name and type. */
internal fun googleSameField(left: FieldReference?, right: FieldReference): Boolean =
    left?.definingClass == right.definingClass && left.name == right.name && left.type == right.type
