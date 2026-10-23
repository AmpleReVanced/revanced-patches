package app.revanced.patches.kakaotalk.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.cloneMutable
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private object ListAdapterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Ljava/util/List;"),
    returnType = "V",
    custom = { _, classDef -> classDef.sourceFile == "ListAdapter.java" },
)

internal data class ListFilterGroup(
    val types: List<String>,
    val settings: List<String> = emptyList(),
)

internal fun BytecodePatchContext.setListFilter(
    extensionType: String,
    groups: List<ListFilterGroup>,
) {
    val extensionClass = mutableClassDefBy(extensionType)
    val stub = extensionClass.methods.single { it.name == "shouldHideItem" }
    val predicate = stub.cloneMutable(
        additionalRegisters = maxOf(0, 2 - stub.implementation!!.registerCount),
    )
    extensionClass.methods.remove(stub)
    extensionClass.methods.add(predicate)
    predicate.addInstructionsWithLabels(
        0,
        buildString {
            groups.forEachIndexed { index, group ->
                group.types.forEach { type ->
                    appendLine("instance-of v0, p0, $type")
                    appendLine("if-nez v0, :filter_$index")
                }
            }
            appendLine("const/4 v0, 0x0")
            appendLine("return v0")
            groups.forEachIndexed { index, group ->
                appendLine(":filter_$index")
                if (group.settings.isEmpty()) {
                    appendLine("const/4 v0, 0x1")
                } else {
                    group.settings.forEachIndexed { settingIndex, setting ->
                        appendLine("invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->$setting()Z")
                        appendLine("move-result v0")
                        if (settingIndex < group.settings.lastIndex) {
                            appendLine("if-nez v0, :filter_result_$index")
                        }
                    }
                }
                appendLine(":filter_result_$index")
                appendLine("return v0")
            }
        },
    )
}

internal fun BytecodePatchContext.filterAdapterItems(
    adapter: MutableClass,
    extensionType: String,
    additionalMethods: List<Method> = emptyList(),
) {
    val submitMethods = ListAdapterFingerprint.originalClassDef.methods.filter {
        it.returnType == "V" && it.parameterTypes in listOf(
            listOf("Ljava/util/List;"),
            listOf("Ljava/util/List;", "Ljava/lang/Runnable;"),
        )
    }
    if (submitMethods.size != 2) throw PatchException("Could not resolve ListAdapter submission methods.")

    (submitMethods + additionalMethods).forEach { superMethod ->
        val existing = adapter.methods.singleOrNull {
            it.name == superMethod.name && it.parameterTypes == superMethod.parameterTypes
        }
        val filter = """
            invoke-static {p1}, $extensionType->filterItems(Ljava/util/List;)Ljava/util/List;
            move-result-object p1
        """
        if (existing != null) {
            existing.addInstructions(0, filter)
        } else {
            adapter.methods.add(
                ImmutableMethod(
                    adapter.type,
                    superMethod.name,
                    superMethod.parameters,
                    "V",
                    AccessFlags.PUBLIC.value,
                    null,
                    null,
                    MutableMethodImplementation(superMethod.parameters.size + 1),
                ).toMutable().apply {
                    val registers = (0..superMethod.parameters.size).joinToString(", ") { "p$it" }
                    addInstructions(
                        0,
                        """
                            $filter
                            invoke-super {$registers}, ${superMethod.smaliReference}
                            return-void
                        """,
                    )
                },
            )
        }
    }
}