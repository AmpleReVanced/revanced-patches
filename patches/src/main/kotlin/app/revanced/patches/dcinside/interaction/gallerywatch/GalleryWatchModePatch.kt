package app.revanced.patches.dcinside.interaction.gallerywatch

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.shared.misc.settings.preference.MORPHE_PREFERENCE_SCREEN_LAYOUT
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.misc.extension.addExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

private const val ITEM_VIEW_FIELD =
    "$RECYCLER_VIEW_HOLDER->itemView:Landroid/view/View;"

@Suppress("unused")
val galleryWatchModePatch = bytecodePatch(
    name = "Gallery watch mode",
    description = "Keeps gallery auto-refresh active, highlights newly arrived posts, and " +
        "optionally alerts with sound or vibration while the gallery is open.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addExtensionPatch, addSettingsPatch, resourceMappingPatch)

    execute {
        addGalleryWatchPreferences()
        setExtensionIsPatchIncluded(EXTENSION_CLASS)
        AutoRefreshDurationFingerprint.method.injectDurationOverride()
        injectGalleryWatchBridges()
        injectGalleryWatchCallbacks()
    }
}

private fun addGalleryWatchPreferences() {
    PreferenceScreen.FEATURES.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_gallery_watch_screen",
            layout = MORPHE_PREFERENCE_SCREEN_LAYOUT,
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference("morphe_pref_gallery_watch_mode", summary = true),
                SwitchPreference("morphe_pref_gallery_watch_sound", summary = true),
                SwitchPreference("morphe_pref_gallery_watch_vibration", summary = true),
            ),
        ),
    )
}

private fun MutableMethod.injectDurationOverride() {
    val returnIndex = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_WIDE)
    val durationRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA

    addInstructionsAtControlFlowLabel(
        returnIndex,
        """
            invoke-static/range {v$durationRegister .. v${durationRegister + 1}}, $EXTENSION_CLASS->overrideAutoRefreshDuration(J)J
            move-result-wide v$durationRegister
        """.trimIndent(),
    )
}

private fun BytecodePatchContext.injectGalleryWatchCallbacks() {
    PostListAdapterConstructorFingerprint.let { fingerprint ->
        fingerprint.method.apply {
            val superConstructorIndex = indexOfFirstInstructionOrThrow(
                methodCall(
                    opcode = Opcode.INVOKE_DIRECT,
                    definingClass = fingerprint.classDef.superclass!!,
                    name = "<init>",
                    parameters = emptyList(),
                    returnType = "V",
                ),
            )
            addInstructions(
                superConstructorIndex + 1,
                "invoke-static/range {p0 .. p1}, $EXTENSION_CLASS->register(Ljava/lang/Object;Ljava/lang/Object;)V",
            )
        }
    }

    AutoRefreshTickFingerprint.method.addInstructionsAtControlFlowLabel(
        AutoRefreshTickFingerprint.instructionMatches.single().index,
        "invoke-static/range {p0 .. p0}, $EXTENSION_CLASS->onAutoRefreshTick(Ljava/lang/Object;)V",
    )

    PostListAdapterUpdateFingerprint.method.addInstructions(
        0,
        "invoke-static/range {p0 .. p2}, $EXTENSION_CLASS->onPostListUpdated(Ljava/lang/Object;Ljava/lang/Object;I)V",
    )

    PostListAdapterBindFingerprint.method.addBeforeReturns(
        "invoke-static/range {p0 .. p2}, $EXTENSION_CLASS->bindPostRow(Ljava/lang/Object;Ljava/lang/Object;I)V",
    )
}

private fun BytecodePatchContext.injectGalleryWatchBridges() {
    val adapterClass = PostListAdapterConstructorFingerprint.classDef
    val adapterUpdate = PostListAdapterUpdateFingerprint.method
    val responseType = adapterUpdate.parameterTypes.first().toString()
    val responseGetter = adapterUpdate.methodReference(
        methodCall(
            definingClass = responseType,
            parameters = emptyList(),
            returnType = "Ljava/util/List;",
        ),
    )
    val modelItemGetter = PostListItemTypeFingerprint.method.methodReference(
        methodCall(parameters = listOf("I"), returnType = "L"),
    )
    val postItemType = modelItemGetter.returnType
    val postItemClass = classDefBy(postItemType)
    val postNumberField = postItemClass.serializedField("no")
    val instanceMethod = { method: Method, _: ClassDef ->
        !AccessFlags.STATIC.isSet(method.accessFlags)
    }
    val adapterItemGetter = Fingerprint(
        parameters = listOf("I"),
        returnType = postItemType,
        filters = listOf(methodCall(modelItemGetter)),
        custom = instanceMethod,
    ).match(adapterClass).method
    val postNumberGetter = Fingerprint(
        parameters = emptyList(),
        returnType = postNumberField.type,
        filters = listOf(fieldAccess(postNumberField)),
        custom = instanceMethod,
    ).match(postItemClass).method

    GetPostsBridgeFingerprint.method.addInstructions(
        0,
        """
            check-cast p0, $responseType
            invoke-virtual {p0}, ${responseGetter.smaliReference}
            move-result-object p0
            return-object p0
        """.trimIndent(),
    )
    GetPostAtBridgeFingerprint.method.addInstructions(
        0,
        """
            check-cast p0, ${adapterClass.type}
            invoke-virtual {p0, p1}, ${adapterItemGetter.smaliReference}
            move-result-object p0
            return-object p0
        """.trimIndent(),
    )
    GetPostNumberBridgeFingerprint.method.addInstructions(
        0,
        """
            check-cast p0, $postItemType
            invoke-virtual {p0}, ${postNumberGetter.smaliReference}
            move-result p0
            return p0
        """.trimIndent(),
    )
    GetItemViewBridgeFingerprint.method.addInstructions(
        0,
        """
            check-cast p0, $RECYCLER_VIEW_HOLDER
            iget-object p0, p0, $ITEM_VIEW_FIELD
            return-object p0
        """.trimIndent(),
    )
}

private fun MutableMethod.addBeforeReturns(instructions: String) {
    findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
        addInstructionsAtControlFlowLabel(index, instructions)
    }
}

private fun Method.methodReference(filter: InstructionFilter) =
    getInstruction(indexOfFirstInstructionOrThrow(filter)).getReference<MethodReference>()!!

private fun ClassDef.serializedField(name: String) = fields.firstOrNull { field ->
    field.annotations.any { annotation ->
        annotation.elements.any { element ->
            element.name == "value" &&
                (element.value as? StringEncodedValue)?.value == name
        }
    }
} ?: throw PatchException("Could not find serialized field $type.$name")