package app.revanced.patches.dcinside.dccon

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import app.revanced.util.registerWidth
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val SETTINGS_CLASS = "Lapp/revanced/extension/dcinside/settings/Settings;"

private const val SET_VISIBILITY_METHOD = "Landroid/view/View;->setVisibility(I)V"

internal data class PostElementMethods(
    val attrInvoke: String,
    val attr: MethodReference,
    val datasetInvoke: String,
    val dataset: MethodReference,
    val removeClassInvoke: String,
    val removeClass: MethodReference,
    val removeInvoke: String,
    val remove: MethodReference,
)

internal fun MutableMethod.inferPostElementMethods(context: BytecodePatchContext): PostElementMethods {
    val elementParameterRegister = parameterRegister(1)

    val attr = instructions.mapNotNull { instruction ->
        instruction.getReference<MethodReference>()?.takeIf { reference ->
            instruction.opcode.isVirtualInvoke &&
                reference.parameterTypes.singleOrNull()?.toString() == "Ljava/lang/String;" &&
                reference.returnType == "Ljava/lang/String;"
        }?.let { reference -> instruction.toNonRangeInvoke() to reference }
    }.groupingBy { it }.eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: throw PatchException("Could not infer post element attr(String) method")

    val dataset = instructions.mapNotNull { instruction ->
        val reference = instruction.getReference<MethodReference>() ?: return@mapNotNull null
        if (!instruction.opcode.isVirtualInvoke ||
            instruction.firstRegister != elementParameterRegister ||
            instruction.argumentRegisterCount != 1 ||
            reference.parameterTypes.isNotEmpty() ||
            reference.returnType != "Ljava/util/Map;"
        ) {
            return@mapNotNull null
        }

        instruction.toNonRangeInvoke() to reference
    }.firstOrNull() ?: throw PatchException("Could not infer post element dataset method")

    val removeClass = inferJsoupRemoveClassReference(context)
    val removeClassInvoke = instructions.firstNotNullOfOrNull { instruction ->
        val reference = instruction.getReference<MethodReference>() ?: return@firstNotNullOfOrNull null
        if (reference.signatureKey == removeClass.signatureKey) instruction.toNonRangeInvoke() else null
    } ?: "invoke-virtual"

    val remove = instructions.mapNotNull { instruction ->
        val reference = instruction.getReference<MethodReference>() ?: return@mapNotNull null
        if (!instruction.opcode.isVirtualInvoke ||
            instruction.firstRegister != elementParameterRegister ||
            instruction.argumentRegisterCount != 1 ||
            reference.parameterTypes.isNotEmpty() ||
            reference.returnType != "V"
        ) {
            return@mapNotNull null
        }

        instruction.toNonRangeInvoke() to reference
    }.firstOrNull() ?: throw PatchException("Could not infer post element remove method")

    return PostElementMethods(
        attrInvoke = attr.first,
        attr = attr.second,
        datasetInvoke = dataset.first,
        dataset = dataset.second,
        removeClassInvoke = removeClassInvoke,
        removeClass = removeClass,
        removeInvoke = remove.first,
        remove = remove.second,
    )
}

internal fun MutableMethod.inferReplyDcconImageFields(holderMethods: Iterable<MutableMethod>): List<FieldReference> {
    val helperReferences = instructions.mapNotNull { instruction ->
        val reference = instruction.getReference<MethodReference>() ?: return@mapNotNull null
        reference.takeIf {
            it.definingClass == definingClass &&
                it.returnType == "V" &&
                (
                    it.parameterTypes.map { parameter -> parameter.toString() } ==
                        listOf("Lcom/dcinside/app/dccon/b;") ||
                        it.parameterTypes.map { parameter -> parameter.toString() } ==
                        listOf("Lcom/dcinside/app/dccon/b;", "Z")
                    )
        }
    }.distinctBy { it.signatureKey }

    val imageFields = helperReferences.mapNotNull { reference ->
        holderMethods.firstOrNull { method -> method.matches(reference) }
            ?.inferLoadedDcconImageField()
    }.distinctBy { it.smaliReference }

    if (imageFields.size != 2) {
        throw PatchException("Could not infer reply DCCon image fields")
    }

    return imageFields
}

internal fun Iterable<Field>.filterDcconViewFields(): List<Field> {
    val fields = filter { field ->
        field.type in setOf(
            "Landroid/view/View;",
            "Landroid/widget/ImageView;",
            "Landroid/widget/TextView;",
        )
    }.toList()

    if (fields.isEmpty()) {
        throw PatchException("Could not infer reply DCCon view fields")
    }

    return fields
}

internal fun List<Field>.toHideVisibilityInstructions(): String =
    joinToString("\n") { field ->
        """
        iget-object p1, p0, ${field.smaliReference}
        invoke-virtual {p1, v0}, $SET_VISIBILITY_METHOD
        """.trimIndent()
    }

internal fun List<FieldReference>.toResizeInstructions(): String =
    joinToString("\n") { field ->
        """
        iget-object v0, p0, ${field.smaliReference}
        invoke-static {v0}, $SETTINGS_CLASS->resizeReplyDcconAsNormal(Landroid/view/View;)V
        """.trimIndent()
    }

private fun MutableMethod.inferLoadedDcconImageField(): FieldReference? {
    instructions.forEachIndexed { index, instruction ->
        val reference = instruction.getReference<MethodReference>() ?: return@forEachIndexed
        if (!instruction.opcode.isVirtualInvoke ||
            reference.parameterTypes.map { it.toString() } !=
            listOf("Landroid/widget/ImageView;", "Ljava/lang/String;")
        ) {
            return@forEachIndexed
        }

        val imageViewRegister = (instruction as? FiveRegisterInstruction)
            ?.takeIf { it.registerCount >= 3 }
            ?.registerD
            ?: return@forEachIndexed

        return instructions.subList(0, index).asReversed().firstNotNullOfOrNull { previousInstruction ->
            val field = previousInstruction.getReference<FieldReference>() ?: return@firstNotNullOfOrNull null
            val registers = previousInstruction as? TwoRegisterInstruction ?: return@firstNotNullOfOrNull null
            if (previousInstruction.opcode == Opcode.IGET_OBJECT &&
                registers.registerA == imageViewRegister &&
                field.type == "Landroid/widget/ImageView;"
            ) {
                field
            } else {
                null
            }
        }
    }

    return null
}

private fun MutableMethod.inferJsoupRemoveClassReference(context: BytecodePatchContext): MethodReference {
    val elementClass = parameterTypes.getOrNull(1)?.toString()
        ?: throw PatchException("Could not inspect post element parameter type")

    val candidates = instructions.mapNotNull { instruction ->
        val reference = instruction.getReference<MethodReference>() ?: return@mapNotNull null
        if (!instruction.opcode.isVirtualInvoke ||
            reference.definingClass != elementClass ||
            reference.parameterTypes.singleOrNull()?.toString() != "Ljava/lang/String;" ||
            reference.returnType != elementClass
        ) {
            return@mapNotNull null
        }

        reference
    }.distinctBy { it.signatureKey }

    return candidates.singleOrNull { reference ->
        reference.isJsoupRemoveClassImplementation(context)
    } ?: throw PatchException("Could not infer post element removeClass(String) method")
}

private fun MethodReference.isJsoupRemoveClassImplementation(context: BytecodePatchContext): Boolean {
    val method = context.classDefByOrNull(definingClass)?.methods?.firstOrNull { method ->
        method.name == name &&
            method.parameterTypes.map { it.toString() } == parameterTypes.map { it.toString() } &&
            method.returnType == returnType
    } ?: return false

    val references = method.implementation?.instructions
        ?.mapNotNull { it.getReference<MethodReference>() }
        ?: return false

    val readsClassSet = references.any { reference ->
        reference.definingClass == definingClass &&
            reference.parameterTypes.isEmpty() &&
            reference.returnType == "Ljava/util/Set;"
    }
    val writesClassSet = references.any { reference ->
        reference.definingClass == definingClass &&
            reference.parameterTypes.singleOrNull()?.toString() == "Ljava/util/Set;" &&
            reference.returnType == definingClass
    }
    val removesFromSet = references.any { reference ->
        reference.definingClass == "Ljava/util/Set;" &&
            reference.name == "remove" &&
            reference.parameterTypes.singleOrNull()?.toString() == "Ljava/lang/Object;" &&
            reference.returnType == "Z"
    }
    val addsToSet = references.any { reference ->
        reference.definingClass == "Ljava/util/Set;" &&
            reference.name == "add" &&
            reference.parameterTypes.singleOrNull()?.toString() == "Ljava/lang/Object;" &&
            reference.returnType == "Z"
    }

    return readsClassSet && writesClassSet && removesFromSet && !addsToSet
}

private fun MutableMethod.matches(reference: MethodReference) =
    definingClass == reference.definingClass &&
        name == reference.name &&
        parameterTypes.map { it.toString() } == reference.parameterTypes.map { it.toString() } &&
        returnType == reference.returnType

private fun MutableMethod.parameterRegister(parameterIndex: Int): Int {
    val implementation = implementation
        ?: throw PatchException("Could not inspect registers for $definingClass->$name")
    val parameterTypes = parameterTypes.map { it.toString() }
    if (parameterIndex !in parameterTypes.indices) {
        throw PatchException("Parameter $parameterIndex is not available in $definingClass->$name")
    }

    val receiverWidth = if (AccessFlags.STATIC.isSet(accessFlags)) 0 else 1
    val parameterWidth = parameterTypes.sumOf { it.registerWidth }
    val firstParameterRegister = implementation.registerCount - receiverWidth - parameterWidth + receiverWidth

    return firstParameterRegister + parameterTypes.take(parameterIndex).sumOf { it.registerWidth }
}

private val MethodReference.signatureKey: String
    get() = "$definingClass->$name(${parameterTypes.joinToString("")})$returnType"

private val Opcode.isVirtualInvoke: Boolean
    get() = this == Opcode.INVOKE_VIRTUAL ||
        this == Opcode.INVOKE_VIRTUAL_RANGE ||
        this == Opcode.INVOKE_INTERFACE ||
        this == Opcode.INVOKE_INTERFACE_RANGE

private fun Instruction.toNonRangeInvoke(): String = when (opcode) {
    Opcode.INVOKE_VIRTUAL,
    Opcode.INVOKE_VIRTUAL_RANGE -> "invoke-virtual"
    Opcode.INVOKE_INTERFACE,
    Opcode.INVOKE_INTERFACE_RANGE -> "invoke-interface"
    else -> throw PatchException("Unsupported invoke opcode $opcode")
}

private val Instruction.firstRegister: Int?
    get() = when (this) {
        is FiveRegisterInstruction -> registerC
        is RegisterRangeInstruction -> startRegister
        else -> null
    }

private val Instruction.argumentRegisterCount: Int?
    get() = when (this) {
        is FiveRegisterInstruction -> registerCount
        is RegisterRangeInstruction -> registerCount
        else -> null
    }
