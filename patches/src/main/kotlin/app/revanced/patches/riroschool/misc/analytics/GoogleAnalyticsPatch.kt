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
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x

internal data class MlKitLogger(val namespace: String, val logger: String, val callable: String, val idTask: String, val voidNames: Set<String>) {
    val prefix: String get() = "Lcom/google/android/gms/internal/$namespace/"
    val owner: String get() = "$prefix$logger;"
}

internal val mlKitLoggers = listOf(
    MlKitLogger("mlkit_code_scanner", "zzny", "zznw", "zzh", setOf("zzb", "zzc")),
    MlKitLogger("mlkit_common", "zzpz", "zzpw", "zzh", setOf("zzb", "zzc", "zzd", "zze", "zzf", "zzg")),
    MlKitLogger("mlkit_vision_common", "zzjt", "zzjq", "zzi", setOf("zza", "zzb")),
)

private val googleAnalyticsBytecodePatch = bytecodePatch(

    description = "Disable ML Kit diagnostics and advertising permissions. Shared DataTransport telemetry is discarded.",
    default = true,
) {
    compatibleWith(riroCompatibility)
    dependsOn(disableTelemetryTransportPatch)
    execute {
        requireRiroTarget(packageMetadata)
        for (config in mlKitLoggers) {
            transformMlKitLogger(mutableClassDefBy(config.owner), config)
            retireMlKitIdCallable(mutableClassDefBy("${config.prefix}${config.callable};"))
        }
        for (factory in listOf("mlkit_code_scanner/zznt", "mlkit_common/zzpt", "mlkit_vision_common/zzjn")) {
            disableMlKitFirelogOption(mutableClassDefBy("Lcom/google/android/gms/internal/$factory;")
                .methods.single { it.name == "zzd" && it.parameterTypes == listOf("Ljava/lang/String;") })
        }
        val startup = mutableClassDefBy("Lcom/rirosoft/riroschool/BaseApplication;").methods.single { it.name == "onCreate" }
        removeMlKitSavedId(startup)
    }
}

/**
 * 역할: ML Kit 진단 logger의 유입·metadata fallback과 ID preload만 비활성화한다.
 * 입력: 검증된 logger 클래스와 해당 계열의 정확한 구성.
 * 출력: 스캐너·모듈·공유 executor를 보존한 변경 가능한 클래스.
 */
internal fun transformMlKitLogger(owner: MutableClass, config: MlKitLogger) {
    require(owner.type == config.owner)
    val voids = owner.methods.filter { it.name in config.voidNames && it.returnType == "V" }
    require(voids.map { it.name }.toSet() == config.voidNames && voids.size == config.voidNames.size) { "ML Kit: logger ingress mismatch" }
    val constructor = owner.methods.single { it.name == "<init>" && it.parameterTypes.size == 4 }
    // constructor의 모든 앵커를 검증한 뒤 복사본에만 변경한다.
    val copy = MutableMethod(constructor)
    removeMlKitIdPreload(copy, config)
    val replacements = voids.map(::retireMlKitVoid).toMutableList()
    if (config.namespace == "mlkit_common") {
        val metadata = MutableMethod(owner.methods.single { it.name == "zzi" && it.returnType == "${config.prefix}zzol;" })
        omitMlKitMetadataId(metadata, config)
        replacements += metadata
    }
    replacements += copy
    for (replacement in replacements) {
        // DexBacked의 CharSequence 참조와 Immutable의 String은 descriptor로 비교한다.
        check(owner.methods.removeIf { it.name == replacement.name && it.returnType == replacement.returnType &&
            it.parameterTypes.map(CharSequence::toString) == replacement.parameterTypes.map(CharSequence::toString) })
        owner.methods.add(replacement)
    }
}

/**
 * 역할: 반환값이 없는 순수 진단 유입 메서드를 완전한 return-void 본문으로 교체한다.
 * 입력: ML Kit logger 안의 검증된 void 메서드.
 * 출력: 기존 서명·접근 권한을 유지하며 진단 side effect가 없는 메서드.
 */
private fun retireMlKitVoid(method: Method): MutableMethod {
    require(method.returnType == "V" && method.name != "<init>" && method.implementation != null)
    val body = ImmutableMethodImplementation(method.implementation!!.registerCount,
        listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)), emptyList(), emptyList())
    return MutableMethod(ImmutableMethod(method.definingClass, method.name, method.parameters, method.returnType,
        method.accessFlags, method.annotations, method.hiddenApiRestrictions, body))
}

/**
 * 역할: logger 생성자의 ID Callable 생성·제출·Task 보관 구간만 삭제한다.
 * 입력: 생성자 복사본과 계열 구성.
 * 출력: 버전 조회 Task와 나머지 초기화가 유지되는 생성자.
 */
private fun removeMlKitIdPreload(method: MutableMethod, config: MlKitLogger) {
    val ins = method.implementation!!.instructions.toList()
    require(method.implementation!!.tryBlocks.isEmpty())
    val type = "${config.prefix}${config.callable};"
    val allocation = ins.indices.single { ins[it].opcode == Opcode.NEW_INSTANCE && (googleReference(ins[it]) as? TypeReference)?.type == type }
    val start = allocation - 3
    val end = allocation + 5
    require((googleReference(ins[start]) as? MethodReference)?.let { it.definingClass == "Lu5/d;" && it.name == "a" } == true &&
        ins[start + 1].opcode == Opcode.MOVE_RESULT_OBJECT &&
        (googleReference(ins[allocation + 3]) as? MethodReference)?.let { it.definingClass == "Lu5/d;" && it.name == "b" } == true &&
        (googleReference(ins[end]) as? FieldReference)?.let { it.definingClass == config.owner && it.name == config.idTask } == true) { "ML Kit: ID preload anchors changed" }
    require(ins.count { (googleReference(it) as? MethodReference)?.let { ref -> ref.definingClass == "Lu5/d;" && ref.name == "b" } == true } == 2)
    method.removeInstructions(start, end - start + 1)
}

/**
 * 역할: common metadata builder에서 ID Task 조회·fallback·ID setter를 제거한다.
 * 입력: zzpz.zzi 복사본과 common logger 구성.
 * 출력: non-ID builder 반환 계약을 보존한 메서드.
 */
private fun omitMlKitMetadataId(method: MutableMethod, config: MlKitLogger) {
    val ins = method.implementation!!.instructions.toList()
    val fields = ins.indices.filter { (googleReference(ins[it]) as? FieldReference)?.let { ref -> ref.definingClass == config.owner && ref.name == config.idTask } == true }
    require(fields.size == 2)
    val start = fields.first()
    val setter = ins.indices.single { (googleReference(ins[it]) as? MethodReference)?.let { ref -> ref.definingClass == "${config.prefix}zzol;" && ref.name == "zzi" } == true }
    require(ins.subList(start, setter).count { (googleReference(it) as? MethodReference)?.let { ref -> ref.definingClass == "Lu5/j;" && ref.name == "a" } == true } == 1)
    method.removeInstructions(start, setter - start + 1)
}

/**
 * 역할: 더 이상 제출되지 않는 ID Callable에서도 getter 직접 호출을 제거한다.
 * 입력: 원본 ID 전용 Callable 클래스.
 * 출력: UUID를 만들거나 읽지 않는 String 반환 Callable.
 */
internal fun retireMlKitIdCallable(owner: MutableClass) {
    val method = owner.methods.single { it.name == "call" && it.parameterTypes.isEmpty() }
    require(method.implementation!!.instructions.count { (googleReference(it) as? MethodReference)?.let { ref -> ref.definingClass == "Lu5/j;" && ref.name == "a" } == true } == 1)
    method.removeInstructions(0, method.implementation!!.instructions.size)
    method.addInstructions(0, "const-string v0, \"\"\nreturn-object v0")
}

/**
 * 역할: 기본 Firelog 옵션의 boolean만 false로 만들고 이벤트 타입 정수는 보존한다.
 * 입력: 세 계열 옵션 factory의 zzd(String) 메서드.
 * 출력: enableFirelog=false, firelogEventType=1인 기본 옵션.
 */
internal fun disableMlKitFirelogOption(method: MutableMethod) {
    val ins = method.implementation!!.instructions.toList()
    val call = ins.indices.single { (googleReference(ins[it]) as? MethodReference)?.let { ref -> ref.name == "zza" && ref.parameterTypes == listOf("Z") } == true }
    require(ins[call - 1].opcode == Opcode.CONST_4 && (googleReference(ins[call + 1]) as? MethodReference)?.parameterTypes == listOf("I"))
    method.replaceInstruction(call - 1, "const/4 v0, 0x0")
    method.addInstructions(call + 1, "const/4 v0, 0x1")
}

/**
 * 역할: 이전 ML Kit 설치 식별자 키만 앱 시작 때 제거한다.
 * 입력: 현재 BaseApplication.onCreate 메서드.
 * 출력: 다른 모델 캐시와 계정·UUID·BLE 설정을 보존한 시작 메서드.
 */
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

/**
 * 역할: DEX 명령에서 참조 피연산자를 읽는다.
 * 입력: 임의 명령.
 * 출력: 참조 명령이면 참조 객체, 아니면 null.
 */
private fun googleReference(instruction: Instruction) = (instruction as? ReferenceInstruction)?.reference

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