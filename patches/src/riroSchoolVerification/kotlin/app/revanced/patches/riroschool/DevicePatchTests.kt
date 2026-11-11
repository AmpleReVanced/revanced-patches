package app.revanced.patches.riroschool

import app.revanced.patches.riroschool.misc.analytics.*
import app.revanced.patches.riroschool.misc.integrity.*
import app.revanced.patches.riroschool.shared.*

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File

/**
 * 역할: 실제 7.5.6 DEX의 탐지 경로 제거와 잘못된 입력 거부를 검증한다.
 * 입력: RIRO_TEST_APK 환경 변수 또는 승인된 원본 APK 경로.
 * 출력: 모든 계약을 만족하면 정상 반환하고 위반하면 예외를 발생시킨다.
 */
fun runDevicePatchTests() {
    val negativeControl = System.getenv("RIRO_DEVICE_NEGATIVE_CONTROL")
    val apk = File(System.getenv("RIRO_TEST_APK") ?: error("Set RIRO_TEST_APK to the original Riro School 7.5.6 APK"))
    val dex = DexFileFactory.loadDexFile(apk, Opcodes.getDefault())
    val classes = dex.classes.associateBy { it.type }
    /** 역할: 정확한 무인자 void 메서드를 찾는다. 입력: 클래스·메서드명. 출력: 유일한 원본 메서드. */
    fun method(owner: String, name: String): Method = classes.getValue(owner).methods.single {
        it.name == name && it.parameterTypes.isEmpty() && it.returnType == "V"
    }
    val activity = "Lcom/rirosoft/riroschool/views/activity/"
    val vpn = method("${activity}p;", "run")
    check(deviceReferences(vpn).any { "getAllNetworks" in it })
    val vpnCopy = MutableMethod(vpn)
    if (negativeControl != "vpn") removeVpnDetection(vpnCopy)
    check(deviceReferences(vpnCopy).none { it.contains("Landroid/net/") })
    check(deviceReferences(vpnCopy).count { "Ld6/b;->a" in it } == 1)
    check(deviceReferences(vpnCopy).count { "IntroActivity;->c()V" in it } == 1)
    check(deviceReferences(vpnCopy).count { "IntroActivity;->e()V" in it } == 1)
    check(deviceReferences(vpnCopy).none { "VPN" in it })
    // 다른 discriminator의 푸시 오류와 공용 listener 생성은 보존해야 한다.
    check(deviceReferences(vpnCopy).filter { "푸시코드" in it } ==
        deviceReferences(vpn).filter { "푸시코드" in it })
    val vpnInstructions = vpn.implementation!!.instructions.toList()
    val vpnStart = vpnInstructions.indexOfFirst { referenceToken(it) == "connectivity" }
    val vpnEnd = vpnInstructions.indexOfFirst { referenceToken(it) == "${activity}IntroActivity;->c()V" }
    check(instructionTokens(vpnCopy) == instructionTokens(vpnInstructions.take(vpnStart) + vpnInstructions.drop(vpnEnd))) {
        "VPN: instructions outside detector region changed"
    }
    checkDeviceControlFlow(vpnCopy)

    val resume = method("${activity}MainActivity;", "onResume")
    check(deviceReferences(resume).any { "isDebuggerConnected" in it })
    check(deviceReferences(resume).any { it == "adb_enabled" })
    val resumeCopy = if (negativeControl == "adb") MutableMethod(resume) else removeDebugDetection(resume)
    check(deviceReferences(resumeCopy).none { "isDebuggerConnected" in it || "adb_enabled" in it })
    check(deviceReferences(resumeCopy).none { "디버깅을 해제" in it })
    check(resumeCopy.implementation!!.tryBlocks.size == resume.implementation!!.tryBlocks.size - 2)
    for (name in listOf("onResume", "startSync", "registerContentObserver", "getUrl", "addOnSuccessListener")) {
        check(deviceReferences(resumeCopy).count { "->$name(" in it } ==
            deviceReferences(resume).count { "->$name(" in it }) { "resume regression: $name" }
    }
    check(deviceReferences(resumeCopy).contains("InAppUpdate"))
    val resumeInstructions = resume.implementation!!.instructions.toList()
    val resumeStart = resumeInstructions.indexOfFirst { referenceToken(it) == "Landroid/os/Debug;->isDebuggerConnected()Z" }
    val resumeEnd = resumeInstructions.indexOfLast { referenceToken(it) == "Landroid/app/Activity;->runOnUiThread(Ljava/lang/Runnable;)V" } + 1
    check(instructionTokens(resumeCopy) == instructionTokens(resumeInstructions.take(resumeStart) + resumeInstructions.drop(resumeEnd))) {
        "ADB: instructions outside detector region changed"
    }
    checkDeviceControlFlow(resumeCopy)

    val intro = MutableMethod(method("${activity}IntroActivity;", "b"))
    check(deviceReferences(intro).any { "isGooglePlayServicesAvailable" in it })
    if (negativeControl != "gms") spoofGmsAvailability(intro)
    check(deviceReferences(intro).none { "isGooglePlayServicesAvailable" in it })
    check(deviceReferences(intro).any { "Landroid/os/Handler;->post" in it })
    checkDeviceControlFlow(intro)

    val malformed = MutableMethod(vpn)
    val badIndex = malformed.implementation!!.instructions.indexOfFirst {
        ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "VPN_CHECK"
    }
    malformed.replaceInstruction(badIndex, "const-string v4, \"unexpected\"")
    val unchanged = deviceReferences(malformed)
    check(runCatching { removeVpnDetection(malformed) }.isFailure)
    check(deviceReferences(malformed) == unchanged) { "failed VPN match partially mutated input" }
    val malformedResume = MutableMethod(resume)
    val usbIndex = malformedResume.implementation!!.instructions.indexOfFirst {
        referenceToken(it)?.contains("설정에서 USB 디버깅을 해제") == true
    }
    malformedResume.replaceInstruction(usbIndex, "const-string v1, \"unexpected\"")
    val unchangedResume = instructionTokens(malformedResume)
    check(runCatching { removeDebugDetection(malformedResume) }.isFailure)
    check(instructionTokens(malformedResume) == unchangedResume)
    check(runCatching { removeVpnDetection(vpnCopy) }.isFailure) { "already patched input accepted" }
    check(runCatching { removeDebugDetection(resumeCopy) }.isFailure) { "already patched resume accepted" }
    check(runCatching { spoofGmsAvailability(intro) }.isFailure) { "already patched GMS accepted" }
}

/**
 * 역할: 검사 결과 비교에 사용할 메서드와 문자열 참조를 순서대로 정규화한다.
 * 입력: 원본 또는 변형된 DEX 메서드.
 * 출력: 인덱스와 주소에 의존하지 않는 참조 목록.
 */
private fun deviceReferences(method: Method): List<String> = method.implementation!!.instructions.mapNotNull {
    referenceToken(it)
}

/**
 * 역할: DEX 참조를 인덱스와 무관한 문자열로 변환한다.
 * 입력: 참조를 포함할 수 있는 명령.
 * 출력: 문자열·메서드·필드 참조 또는 null.
 */
private fun referenceToken(instruction: Instruction): String? =
    when (val ref = (instruction as? ReferenceInstruction)?.reference) {
        is StringReference -> ref.string
        is MethodReference -> "${ref.definingClass}->${ref.name}(${ref.parameterTypes.joinToString("")})${ref.returnType}"
        is FieldReference -> "${ref.definingClass}->${ref.name}:${ref.type}"
        null -> null
        else -> ref.toString()
    }

/** 역할: 메서드 명령을 정규화한다. 입력: 메서드. 출력: 주소에 무관한 명령 목록. */
private fun instructionTokens(method: Method): List<String> = instructionTokens(method.implementation!!.instructions.toList())

/**
 * 역할: DEX 인덱스·재배치 주소·payload 정렬 NOP를 제외한 명령 의미를 비교한다.
 * 입력: 비교할 명령 목록.
 * 출력: opcode·참조·레지스터·상수의 순서 있는 표현.
 */
private fun instructionTokens(instructions: List<Instruction>): List<String> = instructions.filter { it.opcode != Opcode.NOP }.map {
    val registers = when (it) {
        is FiveRegisterInstruction -> listOf(it.registerCount, it.registerC, it.registerD, it.registerE, it.registerF, it.registerG)
        is RegisterRangeInstruction -> listOf(it.startRegister, it.registerCount)
        is ThreeRegisterInstruction -> listOf(it.registerA, it.registerB, it.registerC)
        is TwoRegisterInstruction -> listOf(it.registerA, it.registerB)
        is OneRegisterInstruction -> listOf(it.registerA)
        else -> null
    }
    "${it.opcode}|${referenceToken(it)}|$registers|${(it as? WideLiteralInstruction)?.wideLiteral}"
}

/**
 * 역할: 변형 후 분기와 예외 처리 주소가 실제 명령 경계에 있는지 검증한다.
 * 입력: 변형된 메서드.
 * 출력: 정상 반환 또는 잘못된 제어 흐름의 예외.
 */
private fun checkDeviceControlFlow(method: Method) {
    val implementation = method.implementation!!
    val instructions = implementation.instructions.toList()
    var address = 0
    val byAddress = instructions.associateBy { instruction -> address.also { address += instruction.codeUnits } }
    for ((position, instruction) in byAddress) {
        if (instruction is OffsetInstruction) check(position + instruction.codeOffset in byAddress) {
            "Invalid branch at $position in ${method.name}"
        }
    }
    implementation.tryBlocks.forEach { block ->
        check(block.codeUnitCount > 0 && block.startCodeAddress in byAddress)
        check(block.startCodeAddress + block.codeUnitCount in byAddress || block.startCodeAddress + block.codeUnitCount == address)
        block.exceptionHandlers.forEach { check(it.handlerCodeAddress in byAddress) }
    }
}
