package app.revanced.patches.riroschool

import app.revanced.patches.riroschool.misc.analytics.*
import app.revanced.patches.riroschool.misc.integrity.*
import app.revanced.patches.riroschool.shared.*

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
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

/** Check detector removal and malformed-input rejection against the original APK selected by RIRO_TEST_APK. */
fun runDevicePatchTests() {
    val negativeControl = System.getenv("RIRO_DEVICE_NEGATIVE_CONTROL")
    val apk = File(System.getenv("RIRO_TEST_APK") ?: error("Set RIRO_TEST_APK to the original Riro School 7.5.6 APK"))
    val dex = DexFileFactory.loadDexFile(apk, Opcodes.getDefault())
    val classes = dex.classes.associateBy { it.type }
    /** Find a unique original no-argument void method for the fixed artifact fixture. */
    fun method(owner: String, name: String): Method = classes.getValue(owner).methods.single {
        it.name == name && it.parameterTypes.isEmpty() && it.returnType == "V"
    }
    val activity = "Lcom/rirosoft/riroschool/views/activity/"
    val vpn = method("${activity}p;", "run")
    check(deviceReferences(vpn).any { "getAllNetworks" in it })
    val renamedVpn = MutableMethod(renameDeviceFixture(vpn))
    removeVpnDetection(renamedVpn)
    check(deviceReferences(renamedVpn).none { "VPN_CHECK" in it })
    val vpnCopy = MutableMethod(vpn)
    if (negativeControl != "vpn") removeVpnDetection(vpnCopy)
    check(deviceReferences(vpnCopy).none { it.contains("Landroid/net/") })
    check(deviceReferences(vpnCopy).count { "Ld6/b;->a" in it } == 1)
    check(deviceReferences(vpnCopy).count { "IntroActivity;->c()V" in it } == 1)
    check(deviceReferences(vpnCopy).count { "IntroActivity;->e()V" in it } == 1)
    check(deviceReferences(vpnCopy).none { "VPN" in it })
    // Preserve the other discriminator branches and their shared listeners.
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
    val renamedResume = removeDebugDetection(renameDeviceFixture(resume))
    check(deviceReferences(renamedResume).none { "adb_enabled" in it })
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

    val originalGms = method("${activity}IntroActivity;", "b")
    val renamedGms = MutableMethod(renameDeviceFixture(originalGms))
    spoofGmsAvailability(renamedGms)
    check(deviceReferences(renamedGms).none { "isGooglePlayServicesAvailable" in it })
    val ambiguousGms = MutableMethod(originalGms)
    ambiguousGms.addInstructions(0, "invoke-static {}, Lcom/google/android/gms/common/GoogleApiAvailability;->getInstance()Lcom/google/android/gms/common/GoogleApiAvailability;")
    val ambiguousBefore = instructionTokens(ambiguousGms)
    check(runCatching { spoofGmsAvailability(ambiguousGms) }.isFailure)
    check(instructionTokens(ambiguousGms) == ambiguousBefore)
    val intro = MutableMethod(originalGms)
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

/** Normalize method and string references for stable contract comparisons. */
private fun deviceReferences(method: Method): List<String> = method.implementation!!.instructions.mapNotNull {
    referenceToken(it)
}

/** Represent a DEX reference independently of its global index. */
private fun referenceToken(instruction: Instruction): String? =
    when (val ref = (instruction as? ReferenceInstruction)?.reference) {
        is StringReference -> ref.string
        is MethodReference -> "${ref.definingClass}->${ref.name}(${ref.parameterTypes.joinToString("")})${ref.returnType}"
        is FieldReference -> "${ref.definingClass}->${ref.name}:${ref.type}"
        null -> null
        else -> ref.toString()
    }

/** Normalize executable instructions independently of their addresses. */
private fun instructionTokens(method: Method): List<String> = instructionTokens(method.implementation!!.instructions.toList())

/** Compare instruction semantics while excluding relocated addresses, global indices and payload-alignment NOPs. */
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

/** Ensure every transformed branch and exception handler targets a valid instruction boundary. */
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

/** Rename app-local fixture symbols while retaining complete bytecode reference relationships. */
internal fun renameDeviceFixture(method: Method): Method {
    val app = "Lcom/rirosoft/riroschool/views/activity/"
    val rewriter = com.android.tools.smali.dexlib2.rewriter.DexRewriter(object : com.android.tools.smali.dexlib2.rewriter.RewriterModule() {
        override fun getTypeRewriter(rewriters: com.android.tools.smali.dexlib2.rewriter.Rewriters) =
            com.android.tools.smali.dexlib2.rewriter.Rewriter<String> { type -> when(type) {
                "${app}p;" -> "Lfixture/RenamedRunnable;"
                "Lg6/i;" -> "Lfixture/RenamedIdentity;"
                "Ld6/b;" -> "Lfixture/RenamedConnectivity;"
                "${app}q;" -> "Lfixture/RenamedWarning;"
                else -> type
            } }
        override fun getMethodReferenceRewriter(rewriters: com.android.tools.smali.dexlib2.rewriter.Rewriters) =
            com.android.tools.smali.dexlib2.rewriter.Rewriter<MethodReference> { reference ->
                val rewritten = com.android.tools.smali.dexlib2.rewriter.MethodReferenceRewriter(rewriters).rewrite(reference)
                val rename = reference.definingClass == "Lg6/i;" || reference.definingClass == "Ld6/b;" ||
                    reference.definingClass == "${app}IntroActivity;" && reference.name in listOf("b", "c", "e")
                com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference(rewritten.definingClass,
                    if(rename && reference.name != "<init>") "renamed_${reference.name}" else rewritten.name,
                    rewritten.parameterTypes, rewritten.returnType)
            }
        override fun getFieldReferenceRewriter(rewriters: com.android.tools.smali.dexlib2.rewriter.Rewriters) =
            com.android.tools.smali.dexlib2.rewriter.Rewriter<FieldReference> { reference ->
                val rewritten = com.android.tools.smali.dexlib2.rewriter.FieldReferenceRewriter(rewriters).rewrite(reference)
                com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference(rewritten.definingClass,
                    if(reference.definingClass.startsWith(app)) "renamed_${reference.name}" else rewritten.name, rewritten.type)
            }
    })
    return rewriter.methodRewriter.rewrite(method)
}
