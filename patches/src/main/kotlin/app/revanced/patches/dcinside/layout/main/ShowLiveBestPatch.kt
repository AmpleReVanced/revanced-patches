package app.revanced.patches.dcinside.layout.main

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.classDefByOrNull
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.dcinside.misc.extension.addExtensionPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS = "Lapp/revanced/extension/dcinside/patches/ShowLiveBestPatch;"

@Suppress("unused")
val showLiveBestPatch = patch(
    name = "Show live best on home",
    description = "Shows real-time best posts instead of recommended posts on the home screen.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addExtensionPatch)
    var restoreFeed = false
    var typeSpec = ""

    execute {
        HomeContentsRequestFingerprint.apply {
            val index = instructionMatches.single().index
            restoreFeed = method.getInstruction(index).getReference<StringReference>()!!.string == "main_content_new.php"
            val register = method.getInstruction<OneRegisterInstruction>(index).registerA
            method.replaceInstruction(index, "const-string v$register, \"main_content.php\"")
        }
        if (!restoreFeed) return@execute

        val adapter = MainAdapterFingerprint.classDef
        val itemType = adapter.methods.inferItemTypeGetter(adapter.type)
        val item = classDefBy(itemType.definingClass)
        val types = MainAdapterFingerprint.method.homeComponentTypes()
        typeSpec = listOf(types.recommendedTitle, types.recommendedItem, types.liveFilter,
            types.liveItem, types.liveMore, types.liveRanking, types.spacer).joinToString(",")

        val filterField = adapter.instanceFields.single { it.type.isEnumWith("ALL") }
        val sortField = adapter.instanceFields.single { it.type.isEnumWith("COUNT") }
        val contentsField = adapter.instanceFields.single { field ->
            classDefByOrNull(field.type)?.fields?.any { nested ->
                classDefByOrNull(nested.type)?.fields?.any { it.jsonName == "livebest" } == true
            } == true
        }
        val contents = classDefBy(contentsField.type)
        val modelField = contents.instanceFields.single { field ->
            classDefByOrNull(field.type)?.fields?.any { it.jsonName == "livebest" } == true
        }
        val modelGetter = contents.getterOf(modelField)
        val liveBest = classDefBy(modelField.type).jsonGetter("livebest")
        val postField = item.instanceFields.single { field ->
            classDefByOrNull(field.type)?.fields?.any { it.jsonName == "category" } == true
        }
        val post = classDefBy(postField.type)
        val extension = mutableClassDefBy(EXTENSION_CLASS)

        fun bridge(name: String, body: String, type: String = adapter.type, integer: Boolean = false) {
            val stub = extension.methods.single { it.name == name }
            val target = liveBestMethod(type, "morphe_$name", listOf(type), stub.returnType, 1, body)
            val suffix = if (integer) "" else "-object"
            stub.addInstructions(0, """
                check-cast p0, $type
                invoke-static {p0}, $target
                move-result$suffix p0
                return$suffix p0
            """)
        }
        bridge("getPosts", """
            iget-object p0, p0, ${contentsField.smaliReference}
            invoke-virtual {p0}, $modelGetter
            move-result-object p0
            invoke-virtual {p0}, $liveBest
            move-result-object p0
            return-object p0
        """)
        listOf("getFilter" to filterField, "getSort" to sortField).forEach { (name, field) ->
            bridge(name, "iget-object p0, p0, ${field.smaliReference}\nreturn-object p0")
        }
        bridge("getType", "invoke-virtual {p0}, $itemType\nmove-result p0\nreturn p0", item.type, true)
        val category = post.jsonGetter("category")
        bridge("getCategory", "invoke-virtual {p0}, $category\nmove-result-object p0\nreturn-object p0", post.type)
        val counts = listOf("hit", "recommend", "comment").mapIndexed { index, name ->
            """
                const-string v0, "$name"
                invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :next_$index
                invoke-virtual {p0}, ${post.jsonGetter(name)}
                move-result p0
                return p0
                :next_$index
            """
        }.joinToString("\n")
        val count = liveBestMethod(post.type, "morphe_getLiveBestCount", listOf(post.type, "Ljava/lang/String;"), "I", 3,
            "$counts\nconst/4 v0, 0x0\nreturn v0")
        extension.methods.single { it.name == "getCount" }.addInstructions(0, """
            check-cast p0, ${post.type}
            invoke-static {p0, p1}, $count
            move-result p0
            return p0
        """)

        val filter = item.instanceFields.single { it.type == filterField.type }
        val sort = item.instanceFields.single { it.type == sortField.type }
        val typeField = item.methods.single { it.name == itemType.name }
            .instructions.firstNotNullOf { it.getReference<FieldReference>() }
        val rank = item.methods.single { it.parameterTypes.isEmpty() && it.returnType == "Ljava/lang/Integer;" }
            .instructions.firstNotNullOf { it.getReference<FieldReference>() }
        val countField = item.instanceFields.single { it.type == "Ljava/lang/Integer;" && it.name != rank.name }
        val parameters = listOf("I", post.type, filter.type, sort.type, "Ljava/lang/Integer;", "Ljava/lang/Integer;")
        val constructor = liveBestMethod(item.type, "<init>", parameters, "V", 7, """
            invoke-direct {p0}, Ljava/lang/Object;-><init>()V
            iput p1, p0, $typeField
            iput-object p2, p0, ${postField.smaliReference}
            iput-object p3, p0, ${filter.smaliReference}
            iput-object p4, p0, ${sort.smaliReference}
            iput-object p5, p0, $rank
            iput-object p6, p0, ${countField.smaliReference}
            return-void
        """, constructor = true)
        val factory = liveBestMethod(item.type, "morphe_newLiveBestItem", parameters, item.type, 7, """
            new-instance v0, ${item.type}
            invoke-direct/range {v0 .. v6}, $constructor
            return-object v0
        """)
        extension.methods.single { it.name == "newItem" }.addInstructions(0, """
            check-cast p1, ${post.type}
            check-cast p2, ${filter.type}
            check-cast p3, ${sort.type}
            invoke-static/range {p0 .. p5}, $factory
            move-result-object p0
            return-object p0
        """)
    }

    finalize {
        if (!restoreFeed) return@finalize
        MainAdapterItemsUpdateFingerprint.method.addInstructions(0, """
            const-string v0, "$typeSpec"
            invoke-static {p0, p1, v0}, $EXTENSION_CLASS->replaceRecommendedItems(Ljava/lang/Object;Ljava/util/List;Ljava/lang/String;)Ljava/util/List;
            move-result-object p1
        """)
    }
}

private val Field.jsonName: String?
    get() = annotations.asSequence().flatMap { it.elements.asSequence() }
        .mapNotNull { (it.value as? StringEncodedValue)?.value }.firstOrNull()

private fun String.isEnumWith(name: String) = classDefByOrNull(this)?.let {
    it.superclass == "Ljava/lang/Enum;" && it.methods.any { method ->
        method.name == "<clinit>" && method.instructions.any { instruction ->
            instruction.getReference<StringReference>()?.string == name
        }
    }
} == true

private fun ClassDef.jsonGetter(name: String) = getterOf(fields.single { it.jsonName == name })

private fun ClassDef.getterOf(field: FieldReference): Method = methods.singleOrNull { method ->
    method.parameterTypes.isEmpty() && method.returnType == field.type &&
        method.instructions.mapNotNull { it.getReference<FieldReference>() }.singleOrNull()?.name == field.name
} ?: liveBestMethod(type, "morphe_getLiveBest_${field.name}", emptyList(), field.type, 1,
    "iget${if (field.type.startsWith("L")) "-object" else ""} p0, p0, $field\n" +
        "return${if (field.type.startsWith("L")) "-object" else ""} p0", instance = true)

private fun liveBestMethod(
    owner: String, name: String, parameters: List<String>, result: String, registers: Int,
    body: String, constructor: Boolean = false, instance: Boolean = false,
) = ImmutableMethod(
    owner, name, parameters.map { ImmutableMethodParameter(it, null, null) }, result,
    AccessFlags.PUBLIC.value or when {
        constructor -> AccessFlags.CONSTRUCTOR.value
        instance -> 0
        else -> AccessFlags.STATIC.value
    },
    null, null, MutableMethodImplementation(registers),
).toMutable().apply {
    addInstructions(0, body)
    mutableClassDefBy(owner).methods.add(this)
}