package app.revanced.patches.riroschool.misc.analytics

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.riroschool.shared.requireRiroTarget
import app.revanced.patches.riroschool.shared.riroCompatibility
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal val privacyRuntimePatch = bytecodePatch {
    extendWith("extensions/riroschool.mpe")
}

val disableRirosoftAnalyticsPatch = bytecodePatch(
    name = "Disable Rirosoft analytics",
    description = "Omit native device_id UUID transmission. BLE attendance remains unchanged.",
    default = true,
) {
    compatibleWith(riroCompatibility)
    dependsOn(privacyRuntimePatch)
    execute {
        requireRiroTarget(packageMetadata)
        val methods = nativeIdentityFormsFingerprint().matchAll().map { it.method }
        require(methods.size == 2 && methods.map { it.definingClass }.distinct().size == 1) { "Identity: expected two forms in one helper" }
        val uuidGetter = identityUuidGetterFingerprint().matchAll().single().method
        val replacements = methods.map { MutableMethod(it).also { copy -> omitNativeDeviceId(copy, uuidGetter) } }
        val owner = mutableClassDefBy(methods.first().definingClass)
        for (replacement in replacements) {
            check(owner.methods.removeIf { it.name == replacement.name && it.parameterTypes == replacement.parameterTypes })
            owner.methods.add(replacement)
        }
    }
}

/** Remove the UUID getter and native device_id assembly. Follow the encoded value register instead of relying on obfuscated helper names, and retain all other form fields. */
internal fun omitNativeDeviceId(method: MutableMethod, uuidGetter: MethodReference? = null) {
    require(method.returnType == "[B" && method.parameterTypes.firstOrNull() == "Landroid/content/Context;")
    val instructions = requireNotNull(method.implementation).instructions.toList()
    val fields = instructions.indices.filter {
        ((instructions[it] as? ReferenceInstruction)?.reference as? StringReference)?.string == "&device_id="
    }
    require(fields.size == 1) { "Identity: device_id anchor must be unique" }
    val start = fields.single()
    val encode = instructions.getOrNull(start + 2) as? FiveRegisterInstruction
    require((encode as? ReferenceInstruction)?.reference.let { it as? MethodReference }?.let { it.definingClass == "Ljava/net/URLEncoder;" && it.name == "encode" } == true) { "Identity: UUID encoding call missing" }
    val valueRegister = requireNotNull(encode).registerC
    val calls = (0 until start).filter { index ->
        val ref = (instructions[index] as? ReferenceInstruction)?.reference as? MethodReference
        ref != null && (uuidGetter == null || ref == uuidGetter) && ref.parameterTypes == listOf("Landroid/content/Context;") && ref.returnType == "Ljava/lang/String;" &&
            instructions[index + 1].opcode == Opcode.MOVE_RESULT_OBJECT && (instructions[index + 1] as OneRegisterInstruction).registerA == valueRegister
    }
    require(calls.size == 1) { "Identity: UUID register must have one getter producer" }
    val getter = calls.single()
    require(getter + 1 < start && instructions[getter + 1].opcode == Opcode.MOVE_RESULT_OBJECT)
    val refs = (start + 1..start + 5).map {
        (instructions[it] as? ReferenceInstruction)?.reference
    }
    require((refs[0] as? MethodReference)?.let { it.definingClass == "Ljava/lang/StringBuilder;" && it.name == "append" } == true &&
        (refs[1] as? MethodReference)?.let { it.definingClass == "Ljava/net/URLEncoder;" && it.name == "encode" } == true &&
        instructions[start + 3].opcode == Opcode.MOVE_RESULT_OBJECT &&
        (refs[3] as? MethodReference)?.let { it.definingClass == "Ljava/lang/StringBuilder;" && it.name == "append" } == true &&
        (refs[4] as? StringReference)?.string == "&app_version=") { "Identity: unexpected device_id assembly" }
    method.removeInstructions(start, 5)
    method.removeInstructions(getter, 2)
    filterFinalIdentityForm(method)
}

/** Filter the fully assembled root form immediately before its original getBytes call, preserving its receiver register and encoding behavior. */
private fun filterFinalIdentityForm(method: MutableMethod) {
    val instructions = requireNotNull(method.implementation).instructions.toList()
    val anchors = instructions.indices.filter { index ->
        ((instructions[index] as? ReferenceInstruction)?.reference as? MethodReference)?.let {
            it.definingClass == "Ljava/lang/String;" && it.name == "getBytes" &&
                it.parameterTypes.isEmpty() && it.returnType == "[B"
        } == true
    }
    require(anchors.size == 1) { "Identity: final getBytes anchor must be unique" }
    val index = anchors.single()
    require(instructions[index].opcode == Opcode.INVOKE_VIRTUAL)
    val call = instructions[index] as FiveRegisterInstruction
    require(call.registerCount == 1 && call.registerC <= 255)
    val register = call.registerC
    // Replace at the original label so existing branches cannot skip the filter.
    method.replaceInstruction(index,
        "invoke-static/range {v$register .. v$register}, Lapp/revanced/extension/riroschool/identity/FormParameters;->withoutDeviceId(Ljava/lang/String;)Ljava/lang/String;")
    method.addInstructions(index + 1, """
        move-result-object v$register
        invoke-virtual {v$register}, Ljava/lang/String;->getBytes()[B
    """.trimIndent())
}
