package app.revanced.patches.riroschool

import app.revanced.patches.riroschool.misc.analytics.*

import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.rewriter.DexRewriter
import com.android.tools.smali.dexlib2.rewriter.RewriterModule
import com.android.tools.smali.dexlib2.rewriter.Rewriter
import com.android.tools.smali.dexlib2.rewriter.Rewriters
import java.io.File

/** Checks ID preloading, fallback and diagnostic ingress against original and renamed DEX classes. */
fun runGooglePatchTests() {
    val apk = File(System.getenv("RIRO_TEST_APK") ?: error("Set RIRO_TEST_APK to the original Riro School 7.5.6 APK"))
    val dex = DexFileFactory.loadDexFile(apk, Opcodes.getDefault())
    val classes = dex.classes.associateBy { it.type }
    val loggers = resolveMlKitLoggers(classes.values)
    // Renaming types and their references must not change the semantic logger inventory.
    val privateTypes = loggers.flatMap { listOfNotNull(it.owner, it.callable, it.idGetter.definingClass,
        it.submitMethod.definingClass, it.metadataMethod?.returnType) }.toSet() +
        resolveMlKitFirelogOptions(classes.values).flatMap { listOf(it.definingClass, it.returnType) }
    val renamedTypes = privateTypes.sorted().mapIndexed { index, type -> type to "Lrenamed/fixture/Type$index;" }.toMap()
    val renamed = DexRewriter(object : RewriterModule() {
        override fun getTypeRewriter(rewriters: Rewriters): Rewriter<String> = Rewriter { type ->
            renamedTypes[type] ?: type
        }
    }).dexFileRewriter.rewrite(dex)
    val renamedLoggers = resolveMlKitLoggers(renamed.classes)
    check(renamedLoggers.size == 3 && renamedLoggers.all { it.owner.startsWith("Lrenamed/") })
    check(resolveMlKitFirelogOptions(renamed.classes).size == 3)
    check(runCatching { resolveMlKitLoggers(classes.values.filterNot { it.type == loggers.first().idGetter.definingClass }) }.isFailure)
    check(runCatching { resolveMlKitLoggers(classes.values.toList() + classes.getValue(loggers.first().idGetter.definingClass)) }.isFailure)
    var originalCalls = 0
    for (config in loggers) {
        val owner = MutableClass(classes.getValue(config.owner))
        val constructor = owner.methods.single { it.name == "<init>" }
        val beforeExecutors = constructor.implementation!!.instructions.count { googleTestMethod(it, config.submitMethod) }
        check(beforeExecutors == 2)
        originalCalls += owner.methods.sumOf { method -> method.implementation?.instructions?.count { googleTestMethod(it, config.idGetter) } ?: 0 }
        if (System.getenv("RIRO_GOOGLE_NEGATIVE_CONTROL") != "noop") transformMlKitLogger(owner, config)
        val changedConstructor = owner.methods.single { it.name == "<init>" }
        check(changedConstructor.implementation!!.instructions.count { googleTestMethod(it, config.submitMethod) } == 1) { "ML Kit ID preload still submitted" }
        check(changedConstructor.implementation!!.instructions.none { (it as? ReferenceInstruction)?.reference.let { ref -> ref is TypeReference && ref.type == config.callable } })
        for (method in owner.methods) {
            val instructions = method.implementation?.instructions ?: continue
            check(instructions.none { googleTestMethod(it, config.idGetter) })
            check(instructions.none { googleSameField((it as? ReferenceInstruction)?.reference as? FieldReference, config.idTask) })
            if (method.name in config.voidNames) check(instructions.size == 1 && instructions.single().opcode == Opcode.RETURN_VOID)
        }
        val callable = MutableClass(classes.getValue(config.callable))
        originalCalls += callable.methods.sumOf { it.implementation?.instructions?.count { instruction -> googleTestMethod(instruction, config.idGetter) } ?: 0 }
        retireMlKitIdCallable(callable)
        check(callable.methods.flatMap { it.implementation?.instructions ?: emptyList() }.none { googleTestMethod(it, config.idGetter) })
        check(runCatching { transformMlKitLogger(owner, config) }.isFailure) { "already patched logger accepted" }
    }
    check(originalCalls == 6) { "ML Kit direct getter call inventory changed: $originalCalls" }
    for (factory in resolveMlKitFirelogOptions(classes.values)) {
        val method = MutableMethod(factory)
        disableMlKitFirelogOption(method)
        val booleanCall = method.implementation!!.instructions.indexOfFirst {
            ((it as? ReferenceInstruction)?.reference as? MethodReference)?.parameterTypes == listOf("Z")
        }
        check((method.implementation!!.instructions[booleanCall - 1] as com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction).narrowLiteral == 0)
        check((method.implementation!!.instructions[booleanCall + 1] as com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction).narrowLiteral == 1)
    }
}

/** Compares a call against the fully resolved original method reference. */
private fun googleTestMethod(instruction: com.android.tools.smali.dexlib2.iface.instruction.Instruction, expected: MethodReference): Boolean =
    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.let { googleSameMethod(it, expected) } == true
