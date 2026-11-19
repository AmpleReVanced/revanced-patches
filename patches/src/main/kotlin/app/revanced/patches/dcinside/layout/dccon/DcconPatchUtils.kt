package app.revanced.patches.dcinside.layout.dccon

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val SETTINGS_CLASS = "Lapp/revanced/extension/dcinside/settings/Settings;"
private const val SET_VISIBILITY_METHOD = "Landroid/view/View;->setVisibility(I)V"

internal fun MutableMethod.inferReplyDcconImageFields(): List<FieldReference> {
    val fields = instructions.mapIndexedNotNull { index, instruction ->
        val call = instruction.getReference<MethodReference>() ?: return@mapIndexedNotNull null
        if (instruction.opcode != Opcode.INVOKE_STATIC ||
            call.parameterTypes != listOf("Landroid/widget/ImageView;", "Ljava/lang/String;")
        ) return@mapIndexedNotNull null
        val image = (instruction as FiveRegisterInstruction).registerC
        instructions.subList(0, index).asReversed().firstNotNullOfOrNull {
            val field = it.getReference<FieldReference>()
            field?.takeIf { field ->
                it.opcode == Opcode.IGET_OBJECT && (it as TwoRegisterInstruction).registerA == image &&
                    field.type == "Landroid/widget/ImageView;"
            }
        }
    }.distinctBy { it.smaliReference }
    check(fields.size == 2) { "Could not resolve both reply DCCon image views" }
    return fields
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
