package app.revanced.patches.dcinside.layout.dccon

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.util.cloneMutable
import app.revanced.patches.dcinside.misc.extension.addExtensionPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/revanced/extension/dcinside/patches/PostDcconPatch;"

internal val postDcconPatch = patch {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addExtensionPatch)

    execute {
        val parser = PostDcconImageHandlerFingerprint.method
        val select = parser.instructions.mapNotNull { it.getReference<MethodReference>() }.distinct().single {
            it.parameterTypes.size == 2 && it.parameterTypes[0] == "Ljava/lang/String;" &&
                it.parameterTypes[1].toString().startsWith("Lorg/jsoup/") &&
                classDefBy(it.returnType).superclass == "Ljava/util/ArrayList;"
        }
        val elementType = select.parameterTypes[1].toString()
        val nodeType = classDefBy(elementType).superclass!!
        val attribute = Fingerprint(
            definingClass = nodeType,
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Ljava/lang/String;",
            strings = listOf("abs:"),
        ).originalMethod
        val setAttribute = classDefBy(nodeType).methods.single {
            it.parameterTypes == listOf("Ljava/lang/String;", "Ljava/lang/String;") && it.returnType == "V"
        }
        val remove = Fingerprint(
            definingClass = nodeType,
            parameters = emptyList(),
            returnType = "V",
            filters = listOf(methodCall(definingClass = nodeType, parameters = listOf(nodeType), returnType = "V")),
        ).originalMethod
        val removeClass = Fingerprint(
            definingClass = elementType,
            parameters = listOf("Ljava/lang/String;"),
            returnType = "V",
            filters = listOf(methodCall("Ljava/util/Set;->remove(Ljava/lang/Object;)Z")),
        ).originalMethod
        val extension = mutableClassDefBy(EXTENSION_CLASS)
        val selectStub = extension.methods.single { it.name == "getImages" }
        extension.methods.remove(selectStub)
        extension.methods.add(selectStub.cloneMutable(additionalRegisters = 1).apply {
            addInstructions(
                0,
                """
                    check-cast p0, $elementType
                    const-string v0, "img"
                    invoke-static {v0, p0}, $select
                    move-result-object p0
                    return-object p0
                """,
            )
        })
        mapOf(
            "attribute" to """
                check-cast p0, $nodeType
                invoke-virtual {p0, p1}, $attribute
                move-result-object p0
                return-object p0
            """,
            "setAttribute" to """
                check-cast p0, $nodeType
                invoke-virtual {p0, p1, p2}, $setAttribute
                return-void
            """,
            "removeClass" to """
                check-cast p0, $elementType
                invoke-virtual {p0, p1}, $removeClass
                return-void
            """,
            "removeImage" to """
                check-cast p0, $nodeType
                invoke-virtual {p0}, $remove
                return-void
            """,
        ).forEach { (name, code) -> extension.methods.single { it.name == name }.addInstructions(0, code) }
        parser.addInstruction(0, "invoke-static/range {p1 .. p1}, $EXTENSION_CLASS->processImages(Ljava/lang/Object;)V")
    }
}