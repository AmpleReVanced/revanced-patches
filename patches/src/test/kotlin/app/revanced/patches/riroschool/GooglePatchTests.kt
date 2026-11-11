package app.revanced.patches.riroschool

import app.revanced.patches.riroschool.misc.analytics.*
import app.revanced.patches.riroschool.misc.integrity.*
import app.revanced.patches.riroschool.shared.*

import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import java.io.File

/**
 * 역할: 실제 원본의 ID preload·fallback·진단 유입이 함께 차단되는지 검증한다.
 * 입력: 원본 APK 및 no-op 음성 대조군 환경 변수.
 * 출력: 모든 ML Kit 진단 계약이 충족되면 반환하며 불충족 시 예외다.
 */
fun runGooglePatchTests() {
    val apk = File(System.getenv("RIRO_TEST_APK") ?: error("Set RIRO_TEST_APK to the original Riro School 7.5.6 APK"))
    val dex = DexFileFactory.loadDexFile(apk, Opcodes.getDefault())
    val classes = dex.classes.associateBy { it.type }
    var originalCalls = 0
    for (config in mlKitLoggers) {
        val owner = MutableClass(classes.getValue(config.owner))
        val constructor = owner.methods.single { it.name == "<init>" }
        val beforeExecutors = constructor.implementation!!.instructions.count { googleTestMethod(it, "Lu5/d;", "b") }
        check(beforeExecutors == 2)
        originalCalls += owner.methods.sumOf { method -> method.implementation?.instructions?.count { googleTestMethod(it, "Lu5/j;", "a") } ?: 0 }
        if (System.getenv("RIRO_GOOGLE_NEGATIVE_CONTROL") != "noop") transformMlKitLogger(owner, config)
        val changedConstructor = owner.methods.single { it.name == "<init>" }
        check(changedConstructor.implementation!!.instructions.count { googleTestMethod(it, "Lu5/d;", "b") } == 1) { "ML Kit ID preload still submitted" }
        check(changedConstructor.implementation!!.instructions.none { (it as? ReferenceInstruction)?.reference.let { ref -> ref is TypeReference && ref.type == "${config.prefix}${config.callable};" } })
        for (method in owner.methods) {
            val instructions = method.implementation?.instructions ?: continue
            check(instructions.none { googleTestMethod(it, "Lu5/j;", "a") })
            check(instructions.none { ((it as? ReferenceInstruction)?.reference as? FieldReference)?.let { ref -> ref.definingClass == config.owner && ref.name == config.idTask } == true })
            if (method.name in config.voidNames) check(instructions.size == 1 && instructions.single().opcode == Opcode.RETURN_VOID)
        }
        val callable = MutableClass(classes.getValue("${config.prefix}${config.callable};"))
        originalCalls += callable.methods.sumOf { it.implementation?.instructions?.count { instruction -> googleTestMethod(instruction, "Lu5/j;", "a") } ?: 0 }
        retireMlKitIdCallable(callable)
        check(callable.methods.flatMap { it.implementation?.instructions ?: emptyList() }.none { googleTestMethod(it, "Lu5/j;", "a") })
        check(runCatching { transformMlKitLogger(owner, config) }.isFailure) { "already patched logger accepted" }
    }
    check(originalCalls == 6) { "ML Kit direct getter call inventory changed: $originalCalls" }
    for (factory in listOf("mlkit_code_scanner/zznt", "mlkit_common/zzpt", "mlkit_vision_common/zzjn")) {
        val method = MutableMethod(classes.getValue("Lcom/google/android/gms/internal/$factory;").methods.single { it.name == "zzd" })
        disableMlKitFirelogOption(method)
        val booleanCall = method.implementation!!.instructions.indexOfFirst {
            ((it as? ReferenceInstruction)?.reference as? MethodReference)?.parameterTypes == listOf("Z")
        }
        check((method.implementation!!.instructions[booleanCall - 1] as com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction).narrowLiteral == 0)
        check((method.implementation!!.instructions[booleanCall + 1] as com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction).narrowLiteral == 1)
    }
}

/**
 * 역할: 검사할 호출의 클래스와 이름을 비교한다.
 * 입력: DEX 명령과 기대하는 메서드 클래스·이름.
 * 출력: 해당 호출이면 true.
 */
private fun googleTestMethod(instruction: com.android.tools.smali.dexlib2.iface.instruction.Instruction, owner: String, name: String): Boolean =
    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.let { it.definingClass == owner && it.name == name } == true
