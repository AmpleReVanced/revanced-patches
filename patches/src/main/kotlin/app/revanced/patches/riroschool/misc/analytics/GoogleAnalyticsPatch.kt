package app.revanced.patches.riroschool.misc.analytics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.revanced.patches.riroschool.shared.requireRiroTarget
import app.revanced.patches.riroschool.shared.riroCompatibility
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x

private val googleAnalyticsBytecodePatch = bytecodePatch(

    description = "Disable ML Kit diagnostics and advertising permissions. Shared DataTransport telemetry is discarded.",
    default = true,
) {
    compatibleWith(riroCompatibility)
    dependsOn(disableTelemetryTransportPatch)
    execute {
        requireRiroTarget(packageMetadata)
        val definitions = mutableListOf<ClassDef>()
        classDefForEach { definitions += it }
        // Resolve the complete graph before editing any logger or its Callable.
        val loggers = resolveMlKitLoggers(definitions)
        val options = resolveMlKitFirelogOptions(definitions)
        for (config in loggers) {
            transformMlKitLogger(mutableClassDefBy(config.owner), config)
            retireMlKitIdCallable(mutableClassDefBy(config.callable))
        }
        for (factory in options) {
            disableMlKitFirelogOption(mutableClassDefBy(factory.definingClass).methods.single { googleSameMethod(it, factory) })
        }
        val startup = mutableClassDefBy("Lcom/rirosoft/riroschool/BaseApplication;").methods.single { it.name == "onCreate" }
        removeMlKitSavedId(startup)
    }
}

/** Disables diagnostic ingress, ID metadata fallback and constructor preloading while preserving other initialization. */
internal fun transformMlKitLogger(owner: MutableClass, config: MlKitLogger) {
    require(owner.type == config.owner)
    val voids = owner.methods.filter { it.name in config.voidNames && it.returnType == "V" }
    require(voids.map { it.name }.toSet() == config.voidNames && voids.size == config.voidNames.size) { "ML Kit: logger ingress mismatch" }
    val constructor = owner.methods.single { it.name == "<init>" && it.parameterTypes.size == 4 }
    // Validate constructor anchors before changing the copied method.
    val copy = MutableMethod(constructor)
    removeMlKitIdPreload(copy, config)
    val replacements = voids.map(::retireMlKitVoid).toMutableList()
    if (config.metadataMethod != null) {
        val metadata = MutableMethod(owner.methods.single { googleSameMethod(it, config.metadataMethod) })
        omitMlKitMetadataId(metadata, config)
        replacements += metadata
    }
    replacements += copy
    for (replacement in replacements) {
        // Compare normalized prototypes across DEX-backed and immutable parameter representations.
        check(owner.methods.removeIf { it.name == replacement.name && it.returnType == replacement.returnType &&
            it.parameterTypes.map(CharSequence::toString) == replacement.parameterTypes.map(CharSequence::toString) })
        owner.methods.add(replacement)
    }
}

/** Replaces a verified diagnostic void method with a return while preserving its prototype and access flags. */
private fun retireMlKitVoid(method: Method): MutableMethod {
    require(method.returnType == "V" && method.name != "<init>" && method.implementation != null)
    val body = ImmutableMethodImplementation(method.implementation!!.registerCount,
        listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)), emptyList(), emptyList())
    return MutableMethod(ImmutableMethod(method.definingClass, method.name, method.parameters, method.returnType,
        method.accessFlags, method.annotations, method.hiddenApiRestrictions, body))
}

/** Removes only the ID Callable allocation, submission and task assignment from a constructor copy. */
private fun removeMlKitIdPreload(method: MutableMethod, config: MlKitLogger) {
    val ins = method.implementation!!.instructions.toList()
    require(method.implementation!!.tryBlocks.isEmpty())
    val type = config.callable
    val allocation = ins.indices.single { ins[it].opcode == Opcode.NEW_INSTANCE && (googleReference(ins[it]) as? TypeReference)?.type == type }
    val start = allocation - 3
    val end = allocation + 5
    require((googleReference(ins[start]) as? MethodReference)?.let { it.definingClass == config.submitMethod.definingClass &&
            it.parameterTypes.isEmpty() && it.returnType == config.submitMethod.definingClass } == true &&
        ins[start + 1].opcode == Opcode.MOVE_RESULT_OBJECT &&
        (googleReference(ins[allocation + 3]) as? MethodReference)?.let { googleSameMethod(it, config.submitMethod) } == true &&
        googleSameField(googleReference(ins[end]) as? FieldReference, config.idTask)) { "ML Kit: ID preload anchors changed" }
    require(ins.count { (googleReference(it) as? MethodReference)?.let { ref -> googleSameMethod(ref, config.submitMethod) } == true } == 2)
    method.removeInstructions(start, end - start + 1)
}

/** Removes ID task access, fallback and its resolved metadata setter while preserving the remaining builder. */
private fun omitMlKitMetadataId(method: MutableMethod, config: MlKitLogger) {
    val ins = method.implementation!!.instructions.toList()
    val fields = ins.indices.filter { googleSameField(googleReference(ins[it]) as? FieldReference, config.idTask) }
    require(fields.size == 2)
    val start = fields.first()
    val getter = ins.indices.single { (googleReference(ins[it]) as? MethodReference)?.let { ref -> googleSameMethod(ref, config.idGetter) } == true }
    val setter = getter + 2
    require((googleReference(ins[setter]) as? MethodReference)?.let { ref -> ref.definingClass == method.returnType &&
        ref.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;") && ref.returnType == method.returnType } == true)
    require(ins.subList(start, setter).count { (googleReference(it) as? MethodReference)?.let { ref -> googleSameMethod(ref, config.idGetter) } == true } == 1)
    method.removeInstructions(start, setter - start + 1)
}

/** Retires the ID-only Callable without invoking the preference-backed installation getter. */
internal fun retireMlKitIdCallable(owner: MutableClass) {
    val method = owner.methods.single { it.name == "call" && it.parameterTypes.isEmpty() }
    val calls = googleCalls(method)
    val field = (googleReference(method.implementation!!.instructions.first()) as? FieldReference)
        ?: error("ML Kit: ID Callable field missing")
    require(calls.size == 1 && calls.single().let { it.definingClass == field.type && it.parameterTypes.isEmpty() && it.returnType == "Ljava/lang/String;" })
    method.removeInstructions(0, method.implementation!!.instructions.size)
    method.addInstructions(0, "const-string v0, \"\"\nreturn-object v0")
}

/** Disables the default Firelog boolean while retaining its original event type. */
internal fun disableMlKitFirelogOption(method: MutableMethod) {
    val ins = method.implementation!!.instructions.toList()
    val call = ins.indices.single { (googleReference(ins[it]) as? MethodReference)?.let { ref -> ref.parameterTypes.map(CharSequence::toString) == listOf("Z") } == true }
    require(ins[call - 1].opcode == Opcode.CONST_4 && (googleReference(ins[call + 1]) as? MethodReference)?.parameterTypes == listOf("I"))
    val register = (ins[call - 1] as OneRegisterInstruction).registerA
    method.replaceInstruction(call - 1, "const/4 v$register, 0x0")
    method.addInstructions(call + 1, "const/4 v$register, 0x1")
}

/** Clears only the existing ML Kit installation key at startup, preserving all other preferences. */
private fun removeMlKitSavedId(method: MutableMethod) {
    require(method.implementation!!.registerCount >= 4 && method.implementation!!.instructions.first().opcode == Opcode.INVOKE_SUPER)
    method.addInstructions(1, """
        const-string v0, "com.google.mlkit.internal"
        const/4 v1, 0x0
        invoke-virtual {p0, v0, v1}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
        move-result-object v0
        invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
        move-result-object v0
        const-string v1, "ml_sdk_instance_id"
        invoke-interface {v0, v1}, Landroid/content/SharedPreferences${'$'}Editor;->remove(Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
        move-result-object v0
        invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
    """.trimIndent())
}

@Suppress("unused")
val disableGoogleAnalyticsPatch = resourcePatch(
    name = "Disable Google analytics",
    description = "Disable ML Kit diagnostics and advertising permissions. Shared DataTransport telemetry is discarded.",
    default = true,
) {
    compatibleWith(riroCompatibility)
    dependsOn(googleAnalyticsBytecodePatch)
    execute {
        requireRiroTarget(packageMetadata)
        document("AndroidManifest.xml").use { manifest ->
            val all = manifest.getElementsByTagName("*")
            val nodes = (0 until all.length).map { all.item(it) as org.w3c.dom.Element }
            val permissions = setOf("com.google.android.gms.permission.AD_ID", "android.permission.ACCESS_ADSERVICES_AD_ID", "android.permission.ACCESS_ADSERVICES_ATTRIBUTION")
            val ads = nodes.filter { it.tagName == "uses-permission" && it.getAttribute("android:name") in permissions }
            require(ads.size == 3) { "Advertising permissions: expected all three original declarations" }
            ads.forEach { it.parentNode.removeChild(it) }
            nodes.filter { it.tagName == "property" && it.getAttribute("android:name") == "android.adservices.AD_SERVICES_CONFIG" }
                .forEach { it.parentNode.removeChild(it) }
        }
    }
}
