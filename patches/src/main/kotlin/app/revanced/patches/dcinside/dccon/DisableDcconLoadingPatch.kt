package app.revanced.patches.dcinside.dccon

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import app.revanced.patches.dcinside.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SETTINGS_CLASS = "Lapp/revanced/extension/dcinside/settings/Settings;"
private const val SET_VISIBILITY_METHOD = "Landroid/view/View;->setVisibility(I)V"

@Suppress("unused")
val disableDcconLoadingPatch = bytecodePatch(
    name = "Disable DCCon loading",
    description = "Adds settings to block DCCon image loading in posts and replies.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addSettingsPatch)

    execute {
        val postElementMethods = PostDcconImageHandlerFingerprint.method.inferPostElementMethods()
        PostDcconImageHandlerFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, $SETTINGS_CLASS->blockPostDcconLoading()Z
                move-result v0
                if-eqz v0, :morphe_post_dccon_continue

                move-object/from16 v1, p1
                const-string v0, "src"
                ${postElementMethods.attrInvoke} {v1, v0}, ${postElementMethods.attr}
                move-result-object v0
                invoke-static {v0}, $SETTINGS_CLASS->isDcconUrl(Ljava/lang/String;)Z
                move-result v0
                if-nez v0, :morphe_post_dccon_block

                ${postElementMethods.datasetInvoke} {v1}, ${postElementMethods.dataset}
                move-result-object v0
                const-string v2, "src"
                invoke-interface {v0, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v2
                check-cast v2, Ljava/lang/String;
                invoke-static {v2}, $SETTINGS_CLASS->isDcconUrl(Ljava/lang/String;)Z
                move-result v2
                if-nez v2, :morphe_post_dccon_block

                const-string v2, "original"
                invoke-interface {v0, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v0
                check-cast v0, Ljava/lang/String;
                invoke-static {v0}, $SETTINGS_CLASS->isDcconUrl(Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :morphe_post_dccon_continue

                :morphe_post_dccon_block
                ${postElementMethods.removeInvoke} {v1}, ${postElementMethods.remove}
                return-void

                :morphe_post_dccon_continue
            """.trimIndent(),
        )

        val replyDcconViewFields = ReplyDcconBindFingerprint.classDef.fields
            .filterDcconViewFields()

        ReplyDcconBindFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, $SETTINGS_CLASS->blockReplyDcconLoading()Z
                move-result v0
                if-eqz v0, :morphe_reply_dccon_continue

                const/16 v0, 0x8
                ${replyDcconViewFields.toHideVisibilityInstructions()}
                return-void

                :morphe_reply_dccon_continue
            """.trimIndent(),
        )
    }
}

private data class PostElementMethods(
    val attrInvoke: String,
    val attr: MethodReference,
    val datasetInvoke: String,
    val dataset: MethodReference,
    val removeInvoke: String,
    val remove: MethodReference,
)

private fun MutableMethod.inferPostElementMethods(): PostElementMethods {
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
        removeInvoke = remove.first,
        remove = remove.second,
    )
}

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

private val String.registerWidth: Int
    get() = if (this == "J" || this == "D") 2 else 1

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

private fun Iterable<Field>.filterDcconViewFields(): List<Field> {
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

private fun List<Field>.toHideVisibilityInstructions(): String =
    joinToString("\n") { field ->
        """
        iget-object p1, p0, ${field.smaliReference}
        invoke-virtual {p1, v0}, $SET_VISIBILITY_METHOD
        """.trimIndent()
    }

private val Field.smaliReference: String
    get() = "$definingClass->$name:$type"
