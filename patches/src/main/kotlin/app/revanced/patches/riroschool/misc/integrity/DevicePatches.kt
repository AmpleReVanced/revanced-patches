package app.revanced.patches.riroschool.misc.integrity

import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.revanced.patches.riroschool.shared.requireRiroTarget
import app.revanced.patches.riroschool.shared.riroCompatibility
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
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
        val original = vpnDetectorFingerprint().matchAll().single().method
        val owner = classDefBy(original.definingClass)
        // Validate a copy so a rejected match never edits the input method.
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
        val original = adbDetectorFingerprint().matchAll().single().method
        val owner = classDefBy(original.definingClass)
        val replacement = removeDebugDetection(original)
        val mutableOwner = mutableClassDefBy(owner)
        check(mutableOwner.methods.removeIf { it.name == "onResume" && it.parameterTypes.isEmpty() })
        mutableOwner.methods.add(replacement)
    }
}

/** Remove only the VPN branch while preserving the real connectivity predicate and both startup continuations. Validate every boundary before modifying the copy. */
internal fun removeVpnDetection(method: MutableMethod) {
    require(method.name == "run" && method.parameterTypes.isEmpty() && method.returnType == "V") { "VPN: unexpected method" }
    val implementation = requireNotNull(method.implementation) { "VPN: missing implementation" }
    val ins = implementation.instructions.toList()
    require(implementation.tryBlocks.isEmpty()) { "VPN: unexpected try block" }
    val start = uniqueString(ins, "connectivity")
    require(start >= 3)
    val internet = start - 3
    val internetReference = (ins[internet] as? ReferenceInstruction)?.reference as? MethodReference
    require(ins[internet].opcode == Opcode.INVOKE_STATIC && internetReference?.parameterTypes == listOf("Landroid/content/Context;") && internetReference.returnType == "Z") { "VPN: connectivity predicate missing" }
    val continuations = ins.indices.filter { index ->
        val ref = (ins[index] as? ReferenceInstruction)?.reference as? MethodReference
        index > start && ref?.definingClass == INTRO && ref.parameterTypes.isEmpty() && ref.returnType == "V"
    }
    require(continuations.size == 2) { "VPN: expected online and offline continuations" }
    val (progress, offline) = continuations
    val addresses = IntArray(ins.size + 1)
    ins.forEachIndexed { index, instruction -> addresses[index + 1] = addresses[index] + instruction.codeUnits }
    require((ins[internet + 2] as? OffsetInstruction)?.let { addresses[internet + 2] + it.codeOffset == addresses[offline] } == true) { "VPN: offline branch does not target the preserved continuation" }
    require(start == internet + 3 && ins[internet + 1].opcode == Opcode.MOVE_RESULT &&
        ins[internet + 2].opcode == Opcode.IF_EQZ && progress < offline) { "VPN: invalid internet/progression anchors" }
    require(ins.any { ((it as? ReferenceInstruction)?.reference as? FieldReference)?.let { field ->
        field.definingClass == method.definingClass && field.type == "I"
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

/** Remove debugger/ADB queries, warnings and their dedicated exception handlers. Preserve the rest of the activity lifecycle, WebView and update flow. */
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
    val webView = (ins.getOrNull(end + 1) as? ReferenceInstruction)?.reference as? FieldReference
    require(ins[end].opcode == Opcode.IGET_OBJECT && continuation?.definingClass == MAIN &&
        ins.getOrNull(end + 1)?.opcode == Opcode.IGET_OBJECT && webView?.definingClass == continuation.type &&
        webView.type == "Landroid/webkit/WebView;") { "ADB: continuation binding missing" }
    val warningConstructors = ins.subList(start, end).mapNotNull { (it as? ReferenceInstruction)?.reference as? MethodReference }.filter {
        it.name == "<init>" && it.parameterTypes == listOf(MAIN, "Ljava/lang/String;") && it.returnType == "V"
    }
    require(warningConstructors.size == 2 && warningConstructors.distinct().size == 1) { "ADB: shared warning consumer mismatch" }
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

/** Find the startup GMS result consumer by public API references and spoof that app-local decision only. */
internal fun BytecodePatchContext.spoofGmsAvailability() {
    val method = gmsAvailabilityFingerprint().matchAll().single().method
    spoofGmsAvailability(method)
}

/** Replace the validated availability call/result with SUCCESS in its original result register. */
internal fun spoofGmsAvailability(method: MutableMethod) {
    require(method.definingClass == INTRO && method.parameterTypes.isEmpty() && method.returnType == "V") { "GMS: unexpected method" }
    val ins = requireNotNull(method.implementation).instructions.toList()
    val call = uniqueMethod(ins, "Lcom/google/android/gms/common/GoogleApiAvailability;", "isGooglePlayServicesAvailable", "(Landroid/content/Context;)I")
    val instance = uniqueMethod(ins, "Lcom/google/android/gms/common/GoogleApiAvailability;", "getInstance", "()Lcom/google/android/gms/common/GoogleApiAvailability;")
    require(call == instance + 2 && ins[call + 1].opcode == Opcode.MOVE_RESULT && ins[call + 2].opcode == Opcode.IF_EQZ) { "GMS: availability result consumer mismatch" }
    uniqueMethod(ins, "Landroid/os/Handler;", "post", "(Ljava/lang/Runnable;)Z")
    val resultRegister = (ins[call + 1] as OneRegisterInstruction).registerA
    method.replaceInstruction(call, "const/16 v$resultRegister, 0x0")
    method.removeInstructions(call + 1, 1)
}

/** Find exactly one complete public method reference; missing or duplicate anchors are rejected. */
private fun uniqueMethod(ins: List<Instruction>, owner: String, name: String, descriptor: String): Int {
    val matches = ins.indices.filter { methodMatches(ins[it], owner, name, descriptor) }
    require(matches.size == 1) { "Detector anchor $owner->$name$descriptor: expected 1, found ${matches.size}" }
    return matches.single()
}

/** Compare a method reference using its complete owner, name and descriptor. */
private fun methodMatches(instruction: Instruction, owner: String, name: String, descriptor: String): Boolean {
    val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return ref.definingClass == owner && ref.name == name && "(${ref.parameterTypes.joinToString("")})${ref.returnType}" == descriptor
}

/** Find exactly one matching string anchor and reject missing or ambiguous input. */
private fun uniqueString(ins: List<Instruction>, value: String): Int {
    val matches = ins.indices.filter { stringOf(ins[it]) == value }
    require(matches.size == 1) { "Detector string anchor '$value': expected 1, found ${matches.size}" }
    return matches.single()
}

/** Read a string reference without assuming every instruction has a reference. */
private fun stringOf(instruction: Instruction): String? =
    ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
