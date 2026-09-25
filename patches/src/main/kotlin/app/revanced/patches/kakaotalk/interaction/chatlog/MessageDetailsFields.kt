package app.revanced.patches.kakaotalk.interaction.chatlog

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableAnnotation
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.AnnotationVisibility
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue
import com.android.tools.smali.dexlib2.iface.value.IntEncodedValue
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotation
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotationElement
import com.android.tools.smali.dexlib2.immutable.value.ImmutableStringEncodedValue
import kotlin.metadata.jvm.JvmFieldSignature
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.fieldSignature

private const val FIELD_NAME_ANNOTATION =
    "Lapp/revanced/extension/kakaotalk/chatlog/details/MessageDetailsName;"
private val excludedPackages = listOf(
    "Ljava/", "Landroid/", "Landroidx/", "Lkotlin/", "Lkotlinx/", "Lorg/json/", "Lapp/revanced/",
)
private val classTypePattern = Regex("L[^;<]+;")

context(context: BytecodePatchContext)
internal fun annotateMessageDetailsFields(chatLogClass: ClassDef) {
    val pending = ArrayDeque(messageSubclasses(chatLogClass.type))
    val visited = mutableSetOf<String>()
    while (pending.isNotEmpty()) {
        val type = pending.removeFirst().trimStart('[')
        if (!visited.add(type) || excludedPackages.any(type::startsWith)) continue
        val classDef = context.classDefByOrNull(type) ?: continue
        if (AccessFlags.ENUM.isSet(classDef.accessFlags)) continue

        val names = classDef.kotlinFieldNames()
        if (type == chatLogClass.type && names.isEmpty()) {
            throw PatchException("Could not read ChatLog property metadata.")
        }
        val sourceName = classDef.sourceFile?.substringBeforeLast('.')
        if (sourceName != null || names.isNotEmpty()) {
            context.mutableClassDefBy(classDef).apply {
                sourceName?.let { annotations.add(detailsNameAnnotation(it)) }
                instanceFields.forEach { field ->
                    names[JvmFieldSignature(field.name, field.type)]?.let { name ->
                        field.annotations.add(detailsNameAnnotation(name))
                    }
                }
            }
        }
        classDef.superclass?.let(pending::add)
        classDef.instanceFields.forEach { pending.addAll(it.referencedTypes()) }
    }
}

context(context: BytecodePatchContext)
private fun messageSubclasses(root: String): Set<String> {
    val subclasses = mutableMapOf<String, MutableList<String>>()
    context.classDefForEach { classDef ->
        classDef.superclass?.let { parent -> subclasses.getOrPut(parent, ::mutableListOf).add(classDef.type) }
    }
    val result = linkedSetOf<String>()
    val pending = ArrayDeque(listOf(root))
    while (pending.isNotEmpty()) {
        val type = pending.removeFirst()
        if (result.add(type)) pending.addAll(subclasses[type].orEmpty())
    }
    return result
}

private fun Field.referencedTypes(): List<String> {
    val signature = annotations.firstOrNull { it.type == "Ldalvik/annotation/Signature;" }
        ?.strings("value")?.joinToString("").orEmpty()
    return listOf(type) + classTypePattern.findAll(signature).map { it.value }
}

private fun ClassDef.kotlinFieldNames(): Map<JvmFieldSignature, String> {
    val annotation = annotations.firstOrNull { it.type == "Lkotlin/Metadata;" } ?: return emptyMap()
    val values = annotation.elements.associate { it.name to it.value }
    fun int(name: String, default: Int = 0) = (values[name] as? IntEncodedValue)?.value ?: default
    fun string(name: String) = (values[name] as? StringEncodedValue)?.value.orEmpty()
    val metadata = Metadata(
        kind = int("k", 1),
        metadataVersion = (values["mv"] as? ArrayEncodedValue)?.value.orEmpty()
            .map { (it as IntEncodedValue).value }.toIntArray(),
        data1 = annotation.strings("d1").toTypedArray(),
        data2 = annotation.strings("d2").toTypedArray(),
        extraString = string("xs"),
        packageName = string("pn"),
        extraInt = int("xi"),
    )
    val parsed = KotlinClassMetadata.readLenient(metadata) as? KotlinClassMetadata.Class ?: return emptyMap()
    return parsed.kmClass.properties.mapNotNull { property ->
        property.fieldSignature?.let { it to property.name }
    }.toMap()
}

private fun Annotation.strings(name: String) =
    (elements.firstOrNull { it.name == name }?.value as? ArrayEncodedValue)?.value.orEmpty()
        .map { (it as StringEncodedValue).value }

private fun detailsNameAnnotation(name: String) = MutableAnnotation(
    ImmutableAnnotation(
        AnnotationVisibility.RUNTIME,
        FIELD_NAME_ANNOTATION,
        setOf(ImmutableAnnotationElement("value", ImmutableStringEncodedValue(name))),
    ),
)