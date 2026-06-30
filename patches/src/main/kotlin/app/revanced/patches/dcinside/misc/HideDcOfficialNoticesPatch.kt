package app.revanced.patches.dcinside.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.settings.PreferenceScreen
import app.revanced.patches.dcinside.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

private const val AUTHOR_SERIALIZED_NAME = "name"

@Suppress("unused")
val hideDcOfficialNoticesPatch = bytecodePatch(
    name = "Hide DC official notices",
    description = "Hides official DCInside operator notices from gallery notice lists.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addSettingsPatch)

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            SwitchPreference(
                key = "morphe_pref_hide_official_notices",
                titleKey = "morphe_settings_hide_official_notices",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(OFFICIAL_NOTICE_EXTENSION_CLASS)

        val classDefsByType = mutableMapOf<String, ClassDef>()
        classDefForEach { classDef ->
            classDefsByType[classDef.type] = classDef
        }

        val authorGetter = inferPostItemAuthorGetter(
            GalleryNoticeListHandlerFingerprint.method,
            classDefsByType,
        )

        OfficialNoticeAuthorNameBridgeFingerprint.method.addInstructions(
            0,
            """
                check-cast p0, ${authorGetter.postItemType}
                invoke-virtual {p0}, ${authorGetter.postItemType}->${authorGetter.method.name}()Ljava/lang/String;
                move-result-object p0
                return-object p0
            """.trimIndent(),
        )

        injectGalleryNoticeListFilter(GalleryNoticeListHandlerFingerprint.method)
        MiniNoticeAdapterItemsFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p1}, $OFFICIAL_NOTICE_EXTENSION_CLASS->filterOfficialNotices(Ljava/util/List;)V
            """.trimIndent(),
        )
    }
}

private fun injectGalleryNoticeListFilter(method: MutableMethod) {
    val instructions = method.implementation?.instructions
        ?: throw PatchException("Could not inspect gallery notice list handler")
    val responseType = method.parameterTypes[0].toString()
    val responseListCallIndex = instructions.indexOfFirst { instruction ->
        val reference = instruction.getReference<MethodReference>()
            ?: return@indexOfFirst false

        reference.definingClass == responseType &&
            reference.parameterTypes.isEmpty() &&
            reference.returnType == "Ljava/util/List;"
    }
    if (responseListCallIndex < 0) {
        throw PatchException("Could not find gallery notice response list call")
    }

    val moveResultIndex = responseListCallIndex + 1
    val moveResult = instructions.getOrNull(moveResultIndex)
    if (moveResult?.opcode != Opcode.MOVE_RESULT_OBJECT) {
        throw PatchException("Could not find gallery notice response list result register")
    }

    val listRegister = (moveResult as? OneRegisterInstruction)?.registerA
        ?: throw PatchException("Could not read gallery notice response list result register")

    method.addInstructions(
        moveResultIndex + 1,
        """
            invoke-static {v$listRegister, p3}, $OFFICIAL_NOTICE_EXTENSION_CLASS->filterNoticeList(Ljava/util/List;Ljava/lang/Object;)V
        """.trimIndent(),
    )
}

private data class PostItemAuthorGetter(
    val postItemType: String,
    val method: Method,
)

private fun inferPostItemAuthorGetter(
    method: Method,
    classDefsByType: Map<String, ClassDef>,
): PostItemAuthorGetter {
    val instructions = method.implementation?.instructions
        ?: throw PatchException("Could not inspect gallery notice list handler")

    for (instruction in instructions) {
        if (instruction.opcode != Opcode.CHECK_CAST) {
            continue
        }

        val type = instruction.getReference<TypeReference>()?.type
            ?: continue
        val classDef = classDefsByType[type]
            ?: continue

        val authorField = classDef.fields.firstOrNull { field ->
            field.type == "Ljava/lang/String;" &&
                field.annotations.any { annotation ->
                    annotation.elements.any { element ->
                        element.name == "value" &&
                            (element.value as? StringEncodedValue)?.value == AUTHOR_SERIALIZED_NAME
                    }
                }
        } ?: continue

        val authorGetter = classDef.methods.singleOrNull { candidate ->
            candidate.parameterTypes.isEmpty() &&
                candidate.returnType == authorField.type &&
                candidate.implementation?.instructions?.any { getterInstruction ->
                    val reference = getterInstruction.getReference<FieldReference>()
                        ?: return@any false

                    reference.definingClass == authorField.definingClass &&
                        reference.name == authorField.name &&
                        reference.type == authorField.type
                } == true
        } ?: throw PatchException("Could not infer getter for serialized field \"$AUTHOR_SERIALIZED_NAME\" in $type")

        return PostItemAuthorGetter(type, authorGetter)
    }

    throw PatchException("Could not infer post item author getter for official notice filter")
}
