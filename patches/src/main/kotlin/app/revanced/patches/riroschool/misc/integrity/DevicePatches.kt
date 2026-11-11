package app.revanced.patches.riroschool.misc.integrity

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

private const val ACTIVITY = "Lcom/rirosoft/riroschool/views/activity/"
private const val INTRO = "${ACTIVITY}IntroActivity;"
private const val MAIN = "${ACTIVITY}MainActivity;"

val disableVpnDetectionPatch = bytecodePatch(name = "Disable VPN detection", default = true) {
    compatibleWith(riroCompatibility)
    execute {
        requireRiroTarget(packageMetadata)
        val owner = classDefBy("${ACTIVITY}p;")
        val original = owner.methods.single { it.name == "run" && it.parameterTypes.isEmpty() && it.returnType == "V" }
        // 모든 앵커 검증은 복사본에서 완료한다. 실패한 일치는 원본 클래스를 변경하지 않는다.
        val replacement = MutableMethod(original)
        removeVpnDetection(replacement)
        val mutableOwner = mutableClassDefBy(owner)
        check(mutableOwner.methods.removeIf { it.name == "run" && it.parameterTypes.isEmpty() })
        mutableOwner.methods.add(replacement)
    }
}

val disableAdbDetectionPatch = bytecodePatch(name = "Disable ADB detection", default = true) {
    compatibleWith(riroCompatibility)
    execute {
        requireRiroTarget(packageMetadata)
        val owner = classDefBy(MAIN)
        val original = owner.methods.single { it.name == "onResume" && it.parameterTypes.isEmpty() && it.returnType == "V" }
        val replacement = removeDebugDetection(original)
        val mutableOwner = mutableClassDefBy(owner)
        check(mutableOwner.methods.removeIf { it.name == "onResume" && it.parameterTypes.isEmpty() })
        mutableOwner.methods.add(replacement)
    }
}

/**
 * 역할: 인터넷 확인과 정상 시작 호출을 보존하면서 discriminator 0의 VPN 전용 구간을 삭제한다.
 * 입력: 7.5.6의 p.run 메서드 복사본.
 * 출력: 검증된 VPN 구간을 삭제하며 불일치 또는 중복 앵커는 변경 전에 예외로 거부한다.
 */
internal fun removeVpnDetection(method: MutableMethod) {
    require(method.definingClass == "${ACTIVITY}p;" && method.name == "run") { "VPN: unexpected method" }
    val implementation = requireNotNull(method.implementation) { "VPN: missing implementation" }
    val ins = implementation.instructions.toList()
    require(implementation.tryBlocks.isEmpty()) { "VPN: unexpected try block" }
    val internet = uniqueMethod(ins, "Ld6/b;", "a", "(Landroid/content/Context;)Z")
    val start = uniqueString(ins, "connectivity")
    val progress = uniqueMethod(ins, INTRO, "c", "()V")
    val offline = uniqueMethod(ins, INTRO, "e", "()V")
    require(start == internet + 3 && ins[internet + 1].opcode == Opcode.MOVE_RESULT &&
        ins[internet + 2].opcode == Opcode.IF_EQZ && progress < offline) { "VPN: invalid internet/progression anchors" }
    require(ins.any { ((it as? ReferenceInstruction)?.reference as? FieldReference)?.let { field ->
        field.definingClass == "${ACTIVITY}p;" && field.name == "h" && field.type == "I"
    } == true } && ins.any { it.opcode == Opcode.PACKED_SWITCH }) { "VPN: discriminator missing" }
    for ((owner, name, descriptor) in listOf(
        Triple("Landroid/net/ConnectivityManager;", "getAllNetworks", "()[Landroid/net/Network;"),
        Triple("Landroid/net/ConnectivityManager;", "getNetworkCapabilities", "(Landroid/net/Network;)Landroid/net/NetworkCapabilities;"),
        Triple("Landroid/net/NetworkCapabilities;", "hasTransport", "(I)Z"),
        Triple("Landroid/net/ConnectivityManager;", "getNetworkInfo", "(I)Landroid/net/NetworkInfo;"),
        Triple("Landroid/net/NetworkInfo;", "isConnected", "()Z")
    )) require(uniqueMethod(ins, owner, name, descriptor) in start until progress) { "VPN: detector outside deletion range: $name" }
    for (text in listOf("VPN FOUND: ", "VPN_CHECK", "네트워크 상태 안내")) {
        require(uniqueString(ins, text) in start until progress) { "VPN: dialog/log outside deletion range" }
    }
    val warning = ins.indices.filter { stringOf(ins[it])?.startsWith("VPN을 사용 중일 경우") == true }
    require(warning.size == 1 && warning.single() in start until progress &&
        ins[progress - 1].opcode in setOf(Opcode.GOTO, Opcode.GOTO_16, Opcode.GOTO_32)) { "VPN: unexpected warning boundary" }
    method.removeInstructions(start, progress - start)
}

/**
 * 역할: onResume의 debugger와 adb_enabled 조회·경고 및 해당 두 예외 처리만 제거한다.
 * 입력: 7.5.6 MainActivity.onResume 원본 메서드.
 * 출력: 나머지 생명주기·WebView·학생증·업데이트 흐름을 보존한 새 메서드.
 */
internal fun removeDebugDetection(method: Method): MutableMethod {
    require(method.definingClass == MAIN && method.name == "onResume") { "ADB: unexpected method" }
    val implementation = requireNotNull(method.implementation) { "ADB: missing implementation" }
    val ins = implementation.instructions.toList()
    val start = uniqueMethod(ins, "Landroid/os/Debug;", "isDebuggerConnected", "()Z")
    val adb = uniqueString(ins, "adb_enabled")
    val settings = uniqueMethod(ins, "Landroid/provider/Settings\$Global;", "getInt", "(Landroid/content/ContentResolver;Ljava/lang/String;I)I")
    val debuggerWarning = uniqueString(ins, "안전한 리로스쿨 사용을 위해\n설정에서 유무선 디버깅을 해제한 후\n다시 이용해 주세요.")
    val adbWarning = uniqueString(ins, "안전한 리로스쿨 사용을 위해\n설정에서 USB 디버깅을 해제한 후\n다시 이용해 주세요.")
    require(start < debuggerWarning && debuggerWarning < adb && settings == adb + 1 && settings < adbWarning) { "ADB: invalid detector ordering" }
    val uiCalls = ins.indices.filter { methodMatches(ins[it], "Landroid/app/Activity;", "runOnUiThread", "(Ljava/lang/Runnable;)V") }
    require(uiCalls.size == 2 && uiCalls[0] == debuggerWarning + 2 && uiCalls[1] == adbWarning + 2) { "ADB: unexpected warning producer" }
    val end = uiCalls[1] + 1
    val continuation = (ins[end] as? ReferenceInstruction)?.reference as? FieldReference
    require(ins[end].opcode == Opcode.IGET_OBJECT && continuation?.definingClass == MAIN && continuation.name == "I") { "ADB: continuation binding missing" }
    require(ins.subList(start, end).count { methodMatches(it, "${ACTIVITY}q;", "<init>", "(${MAIN}Ljava/lang/String;)V") } == 2) { "ADB: shared warning consumer mismatch" }
    val addresses = IntArray(ins.size + 1)
    ins.forEachIndexed { index, instruction -> addresses[index + 1] = addresses[index] + instruction.codeUnits }
    val startAddress = addresses[start]
    val endAddress = addresses[end]
    val detectorTries = implementation.tryBlocks.filter { block ->
        block.startCodeAddress >= startAddress && block.startCodeAddress + block.codeUnitCount <= endAddress &&
            block.exceptionHandlers.all { it.handlerCodeAddress in startAddress until endAddress }
    }
    require(detectorTries.size == 2 && detectorTries.all { block ->
        block.exceptionHandlers.size == 1 && block.exceptionHandlers.single().exceptionType == "Ljava/lang/Exception;"
    }) { "ADB: expected exactly two dedicated Exception handlers" }
    val retainedTries = implementation.tryBlocks.filterNot { it in detectorTries }
    require(retainedTries.none { block ->
        block.startCodeAddress < endAddress && block.startCodeAddress + block.codeUnitCount > startAddress ||
            block.exceptionHandlers.any { it.handlerCodeAddress in startAddress until endAddress }
    }) { "ADB: shared handler intersects detector range" }
    val filtered = ImmutableMethodImplementation(implementation.registerCount, ins, retainedTries, implementation.debugItems)
    val replacement = MutableMethod(ImmutableMethod(method.definingClass, method.name, method.parameters,
        method.returnType, method.accessFlags, method.annotations, method.hiddenApiRestrictions, filtered))
    replacement.removeInstructions(start, end - start)
    return replacement
}

/**
 * 역할: 시작 화면의 GMS 가용성 소비 지점만 정상값 0으로 고정한다.
 * 입력: 현재 패치 컨텍스트의 IntroActivity.b.
 * 출력: 실제 가용성 조회를 실행하지 않는 앱 전용 시작 판정.
 */
internal fun BytecodePatchContext.spoofGmsAvailability() {
    val method = mutableClassDefBy(INTRO).methods.single { it.name == "b" && it.parameterTypes.isEmpty() && it.returnType == "V" }
    spoofGmsAvailability(method)
}

/**
 * 역할: 검증된 GMS availability 호출과 move-result를 상수 명령으로 교체한다.
 * 입력: 7.5.6 IntroActivity.b의 변경 가능한 메서드.
 * 출력: 동일 결과 레지스터에 SUCCESS/0을 기록한 메서드.
 */
internal fun spoofGmsAvailability(method: MutableMethod) {
    require(method.definingClass == INTRO && method.name == "b") { "GMS: unexpected method" }
    val ins = requireNotNull(method.implementation).instructions.toList()
    val call = uniqueMethod(ins, "Lcom/google/android/gms/common/GoogleApiAvailability;", "isGooglePlayServicesAvailable", "(Landroid/content/Context;)I")
    val instance = uniqueMethod(ins, "Lcom/google/android/gms/common/GoogleApiAvailability;", "getInstance", "()Lcom/google/android/gms/common/GoogleApiAvailability;")
    require(call == instance + 2 && ins[call + 1].opcode == Opcode.MOVE_RESULT && ins[call + 2].opcode == Opcode.IF_EQZ) { "GMS: availability result consumer mismatch" }
    uniqueMethod(ins, "Landroid/os/Handler;", "post", "(Ljava/lang/Runnable;)Z")
    val resultRegister = (ins[call + 1] as OneRegisterInstruction).registerA
    method.replaceInstruction(call, "const/16 v$resultRegister, 0x0")
    method.removeInstructions(call + 1, 1)
}

/**
 * 역할: 정확한 메서드 참조를 단일 앵커로 찾는다.
 * 입력: 명령 목록과 클래스·이름·DEX 서명.
 * 출력: 유일한 명령 인덱스이며 누락·중복은 예외다.
 */
private fun uniqueMethod(ins: List<Instruction>, owner: String, name: String, descriptor: String): Int {
    val matches = ins.indices.filter { methodMatches(ins[it], owner, name, descriptor) }
    require(matches.size == 1) { "Detector anchor $owner->$name$descriptor: expected 1, found ${matches.size}" }
    return matches.single()
}

/**
 * 역할: 명령의 메서드 참조를 전체 서명으로 비교한다.
 * 입력: 명령과 예상 클래스·이름·서명.
 * 출력: 참조가 정확히 일치할 때 true.
 */
private fun methodMatches(instruction: Instruction, owner: String, name: String, descriptor: String): Boolean {
    val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return ref.definingClass == owner && ref.name == name && "(${ref.parameterTypes.joinToString("")})${ref.returnType}" == descriptor
}

/**
 * 역할: 정확히 일치하는 문자열 앵커의 유일성을 검사한다.
 * 입력: 명령 목록과 기대 문자열.
 * 출력: 유일한 인덱스이며 불일치하면 예외다.
 */
private fun uniqueString(ins: List<Instruction>, value: String): Int {
    val matches = ins.indices.filter { stringOf(ins[it]) == value }
    require(matches.size == 1) { "Detector string anchor '$value': expected 1, found ${matches.size}" }
    return matches.single()
}

/**
 * 역할: 명령에서 문자열 참조만 안전하게 읽는다.
 * 입력: 임의 DEX 명령.
 * 출력: 문자열 또는 해당하지 않으면 null.
 */
private fun stringOf(instruction: Instruction): String? =
    ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
