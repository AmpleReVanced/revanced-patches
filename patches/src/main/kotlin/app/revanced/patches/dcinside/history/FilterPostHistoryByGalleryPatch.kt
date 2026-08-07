package app.revanced.patches.dcinside.history

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.asSequence
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import app.morphe.util.traverseClassHierarchy
import app.revanced.patches.dcinside.misc.addExtensionPatch
import app.revanced.patches.dcinside.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.revanced.util.parameterTypeNames
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import org.w3c.dom.Element

private fun MutableClass.hostMethod(name: String, registerCount: Int): MutableMethod =
    ImmutableMethod(
        type,
        name,
        emptyList(),
        "V",
        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
        null,
        null,
        MutableMethodImplementation(registerCount),
    ).toMutable()

private const val REALM_QUERY_CLASS = "Lio/realm/RealmQuery;"
private const val GALLERY_ID_COLUMN = "galleryId"

private const val FILTER_VIEW_ID = "morphe_post_history_gallery_filter"
private const val EDIT_VIEW_ID = "post_history_edit"
private const val ROW_ANCHOR_VIEW_ID = "post_history_write_title"

private val filterPostHistoryByGalleryBytecodePatch = bytecodePatch {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addExtensionPatch, addSettingsPatch)

    execute {
        val postHistoryClass = PostHistoryRealmProxyFingerprint.originalClassDef.superclass
            ?: throw PatchException("Could not find the recently viewed post model")
        val (galleryId, galleryName) = PostHistoryRealmProxyFingerprint.instructionMatches
            .mapNotNull { it.instruction.getReference<MethodReference>() }

        fun Fingerprint.returnAccessor(accessor: MethodReference) = method.addInstructions(
            0,
            """
                check-cast p0, $postHistoryClass
                invoke-virtual {p0}, $postHistoryClass->${accessor.name}()Ljava/lang/String;
                move-result-object p0
                return-object p0
            """.trimIndent(),
        )

        GalleryIdBridgeFingerprint.returnAccessor(galleryId)
        GalleryNameBridgeFingerprint.returnAccessor(galleryName)

        // Matched exhaustively, as the query is anchored on its shape and the types it
        // declares drive the field read and the call the collect bridge below emits.
        val queryMatch = PostHistoryQueryFingerprint.matchSingle()
        val queryMethod = queryMatch.originalMethod
        val realmType = queryMethod.parameterTypes.first().toString()
        val itemManagerType = queryMethod.definingClass

        // Selecting every post deletes through this query instead of the displayed list,
        // so the gallery has to narrow it as well or a filtered delete drops the history.
        val selectionQuery = mutableClassDefBy(itemManagerType).methods.single { method ->
            method.parameterTypes.map { it.toString() } == listOf(realmType)
        }
        val realmQueryIn = selectionQuery.instructions.firstNotNullOf { instruction ->
            instruction.getReference<MethodReference>()?.takeIf { reference ->
                reference.definingClass == REALM_QUERY_CLASS &&
                    reference.parameterTypeNames == listOf("Ljava/lang/String;", "[Ljava/lang/String;")
            }
        }

        // Narrowed on the query itself, so that every consumer of it stays in agreement.
        listOf(queryMatch.method, selectionQuery).forEach { query ->
            query.apply {
                val whereIndex = indexOfFirstInstructionOrThrow(
                    methodCall(
                        definingClass = realmType,
                        parameters = listOf("Ljava/lang/Class;"),
                        returnType = REALM_QUERY_CLASS,
                    ),
                )
                if (getInstruction(whereIndex + 1).opcode != Opcode.MOVE_RESULT_OBJECT) {
                    throw PatchException("Could not find the recently viewed posts query")
                }

                val queryRegister = getInstruction<OneRegisterInstruction>(whereIndex + 1).registerA
                val freeRegisters = getFreeRegisterProvider(whereIndex + 2, 2, queryRegister)
                val galleriesRegister = freeRegisters.getFreeRegister4Bit()
                val columnRegister = freeRegisters.getFreeRegister4Bit()

                addInstructionsWithLabels(
                    whereIndex + 2,
                    """
                        invoke-static {}, $GALLERY_FILTER_EXTENSION_CLASS->activeGalleryFilter()[Ljava/lang/String;
                        move-result-object v$galleriesRegister
                        if-eqz v$galleriesRegister, :unfiltered
                        const-string v$columnRegister, "$GALLERY_ID_COLUMN"
                        invoke-virtual {v$queryRegister, v$columnRegister, v$galleriesRegister}, ${realmQueryIn.smaliReference}
                        move-result-object v$queryRegister
                        :unfiltered
                        nop
                    """.trimIndent(),
                )
            }
        }

        val reloadMethod = PostHistoryReloadFingerprint.originalMethod

        // The reload and the query are not reachable from the extension, so they are
        // exposed through an interface the recently viewed posts activity implements.
        val activityClass = mutableClassDefBy(POST_HISTORY_ACTIVITY_CLASS)
        val itemManagerField = activityClass.fields.firstOrNull { it.type == itemManagerType }
            ?: throw PatchException("Could not find the recently viewed posts item manager")

        // The realm is opened by a base activity, so the hierarchy is searched for its getter.
        var realmGetterOrNull: Method? = null
        traverseClassHierarchy(activityClass) {
            if (realmGetterOrNull != null) return@traverseClassHierarchy

            realmGetterOrNull = methods.firstOrNull { method ->
                method.parameterTypes.isEmpty() &&
                    method.returnType == realmType &&
                    !AccessFlags.STATIC.isSet(method.accessFlags) &&
                    !AccessFlags.PRIVATE.isSet(method.accessFlags)
            }
        }
        val realmGetter = realmGetterOrNull
            ?: throw PatchException("Could not find the recently viewed posts realm")

        activityClass.apply {
            interfaces.add(GALLERY_FILTER_HOST_INTERFACE)

            methods.add(
                hostMethod("patch_reloadPostHistory", 1).apply {
                    addInstructions(
                        0,
                        """
                            invoke-direct {p0}, ${reloadMethod.smaliReference}
                            return-void
                        """.trimIndent(),
                    )
                },
            )

            methods.add(
                hostMethod("patch_collectGalleries", 8).apply {
                    addInstructionsWithLabels(
                        0,
                        """
                            invoke-virtual {p0}, ${realmGetter.smaliReference}
                            move-result-object v2
                            if-eqz v2, :done
                            iget-object v1, p0, $type->${itemManagerField.name}:$itemManagerType
                            if-eqz v1, :done
                            const-wide/16 v3, 0x0
                            const-wide v5, 0x7fffffffffffffffL
                            invoke-virtual/range {v1 .. v6}, $itemManagerType->${queryMethod.name}(${realmType}JJ)Ljava/util/List;
                            move-result-object v0
                            invoke-static {v0}, $GALLERY_FILTER_EXTENSION_CLASS->collectGalleries(Ljava/util/List;)V
                            :done
                            return-void
                        """.trimIndent(),
                    )
                },
            )
        }

        PostHistoryActivityOnCreateFingerprint.method.apply {
            addInstruction(
                indexOfFirstInstructionOrThrow(methodCall(reloadMethod)),
                "invoke-static {p0}, $GALLERY_FILTER_EXTENSION_CLASS->initialize($GALLERY_FILTER_HOST_INTERFACE)V",
            )
        }
    }
}

@Suppress("unused")
val filterPostHistoryByGalleryPatch = resourcePatch(
    name = "Filter recently viewed posts by gallery",
    description = "Adds a gallery filter below the recently viewed posts filters, " +
        "which narrows the list down to the posts of a single gallery.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(filterPostHistoryByGalleryBytecodePatch)

    execute {
        document("res/values/ids.xml").use { document ->
            val exists = document.getElementsByTagName("*")
                .asSequence()
                .any { it.attributes?.getNamedItem("name")?.nodeValue == FILTER_VIEW_ID }

            if (!exists) {
                document.documentElement.appendChild(
                    document.createElement("item").apply {
                        setAttribute("type", "id")
                        setAttribute("name", FILTER_VIEW_ID)
                    },
                )
            }
        }

        document("res/layout/activity_post_history.xml").use { document ->
            val views = document.getElementsByTagName("*")
                .asSequence()
                .mapNotNull { it as? Element }
                .toList()

            if (views.any { it.getAttribute("android:id") == "@id/$FILTER_VIEW_ID" }) return@use

            val editButton = views.firstOrNull {
                it.getAttribute("android:id") == "@id/$EDIT_VIEW_ID"
            } ?: error("Could not find $EDIT_VIEW_ID in activity_post_history.xml")

            // The filter row is already full, so the gallery filter gets its own row
            // between the filter row and the list, styled after the edit button.
            val galleryFilter = editButton.cloneNode(true) as Element
            galleryFilter.apply {
                setAttribute("android:id", "@id/$FILTER_VIEW_ID")
                setAttribute("android:text", "@string/$FILTER_VIEW_ID")
                setAttribute("android:textAppearance", "?attr/textTypeSub")
                setAttribute("android:maxWidth", "200.0dp")
                setAttribute("android:ellipsize", "end")
                setAttribute("android:paddingTop", "4.0dp")
                setAttribute("android:paddingBottom", "4.0dp")
                setAttribute("android:layout_marginStart", "10.0dp")
                setAttribute("android:layout_marginTop", "4.0dp")
                removeAttribute("android:layout_marginEnd")
                removeAttribute("app:layout_constraintEnd_toEndOf")
                removeAttribute("app:layout_constraintBottom_toBottomOf")
                removeAttribute("app:layout_constraintTop_toTopOf")
                setAttribute("app:layout_constraintStart_toStartOf", "parent")
                setAttribute("app:layout_constraintTop_toBottomOf", "@id/$ROW_ANCHOR_VIEW_ID")
            }

            // The row divider and the list follow the gallery filter, so that it reads
            // as part of the filter header instead of the list.
            views.filter {
                it.getAttribute("app:layout_constraintTop_toBottomOf") == "@id/$ROW_ANCHOR_VIEW_ID"
            }.forEach {
                it.setAttribute("app:layout_constraintTop_toBottomOf", "@id/$FILTER_VIEW_ID")
                it.setAttribute("android:layout_marginTop", "8.0dp")
            }

            editButton.parentNode.insertBefore(galleryFilter, editButton.nextSibling)
        }
    }
}
