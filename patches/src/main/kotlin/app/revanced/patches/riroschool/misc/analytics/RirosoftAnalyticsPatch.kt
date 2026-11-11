package app.revanced.patches.riroschool.misc.analytics

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.revanced.patches.riroschool.shared.requireRiroTarget
import app.revanced.patches.riroschool.shared.riroCompatibility
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
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
        val owner = mutableClassDefBy("Lg6/i;")
        val replacements = listOf("g", "h").map { name ->
            MutableMethod(owner.methods.single { it.name == name && it.returnType == "[B" }).also(::omitNativeDeviceId)
        }
        for (replacement in replacements) {
            check(owner.methods.removeIf { it.name == replacement.name && it.parameterTypes == replacement.parameterTypes })
            owner.methods.add(replacement)
        }
    }
}

/**
 * 역할: UUID를 읽는 호출과 device_id 키·값을 붙이는 구간만 제거한다.
 * 입력: 7.5.6 g6.i.g 또는 g6.i.h의 변경 가능한 폼 생성기.
 * 출력: 다른 폼 필드와 로컬 UUID 저장 계약을 유지하는 메서드.
 */
internal fun omitNativeDeviceId(method: MutableMethod) {
    require(method.definingClass == "Lg6/i;" && method.name in listOf("g", "h") && method.returnType == "[B")
    val instructions = requireNotNull(method.implementation).instructions.toList()
    val calls = instructions.indices.filter {
        ((instructions[it] as? ReferenceInstruction)?.reference as? MethodReference)?.let { ref ->
            ref.definingClass == "Lg6/i;" && ref.name == "j" &&
                ref.parameterTypes == listOf("Landroid/content/Context;") && ref.returnType == "Ljava/lang/String;"
        } == true
    }
    val fields = instructions.indices.filter {
        ((instructions[it] as? ReferenceInstruction)?.reference as? StringReference)?.string == "&device_id="
    }
    require(calls.size == 1 && fields.size == 1) { "Identity: UUID and device_id anchors must be unique" }
    val getter = calls.single()
    val start = fields.single()
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

/**
 * 역할: 추가 폼 문자열까지 조립한 최종 본문을 바이트 변환 직전에 필터링한다.
 * 입력: 내장 device_id 조립을 제거한 g6.i.g 또는 g6.i.h 메서드.
 * 출력: 원래 본문 레지스터를 같은 타입의 필터 결과로 교체한 메서드.
 */
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
    // 기존 분기 라벨이 필터를 건너뛰지 않도록 원래 위치의 명령을 교체한다.
    method.replaceInstruction(index,
        "invoke-static/range {v$register .. v$register}, Lapp/revanced/extension/riroschool/identity/FormParameters;->withoutDeviceId(Ljava/lang/String;)Ljava/lang/String;")
    method.addInstructions(index + 1, """
        move-result-object v$register
        invoke-virtual {v$register}, Ljava/lang/String;->getBytes()[B
    """.trimIndent())
}
