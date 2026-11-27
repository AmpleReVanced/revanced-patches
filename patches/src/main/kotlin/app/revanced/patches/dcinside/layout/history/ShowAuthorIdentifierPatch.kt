package app.revanced.patches.dcinside.layout.history

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getMutableMethod
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.misc.extension.addExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.revanced.util.parameterTypeNames
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

@Suppress("unused")
val showAuthorIdentifierPatch = patch(
    name = "Show author identifier",
    description = "Adds options to show the author identifier next to the nickname in posts, " +
        "post lists, comments, and the recently-viewed posts list. The recently-viewed list only " +
        "shows it for posts opened after this patch is installed.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addExtensionPatch, addSettingsPatch)

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            SwitchPreference(
                key = "morphe_pref_show_post_author_identifier",
                titleKey = "morphe_settings_show_post_author_identifier",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_show_comment_author_identifier",
                titleKey = "morphe_settings_show_comment_author_identifier",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_show_post_history_author_identifier",
                titleKey = "morphe_settings_show_post_history_author_identifier",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val postInfo = classDefBy(POST_INFO_CLASS)
        val nameGetters = postInfo.gettersOf("name").map { method -> method.name }.toSet()
        val userIdGetter = postInfo.gettersOf("user_id").first()
        val ipGetter = postInfo.gettersOf("ip").first()

        val authorLineBuilders = PostAuthorLineFingerprint.instructionMatches.single().instruction
            .getReference<MethodReference>()!!.definingClass
            .let { builderClass ->
                classDefBy(builderClass).methods.filter {
                    it.returnType == "Landroid/text/SpannableStringBuilder;" &&
                        it.parameterTypes.firstOrNull() == "Landroid/content/Context;"
                }.map { it.smaliReference }.toSet()
            }

        // Posts and post lists render the author line in obfuscated classes, so every method that
        // builds one is patched instead of fingerprinting each of them.
        listOf(POST_INFO_CLASS, POST_ITEM_CLASS).forEach { modelType ->
            val model = classDefBy(modelType)
            val modelUserIdGetter = model.gettersOf("user_id").first()

            model.gettersOf("name").forEach { nicknameGetter ->
                val bridge = authorNameBridge(modelType, nicknameGetter, modelUserIdGetter, "formatPostAuthorName")
                methodCall(nicknameGetter).matchAllMethodIndicesForEach(requireMatches = false) { index ->
                    if (instructions.none { it.getReference<MethodReference>()?.smaliReference in authorLineBuilders }) {
                        return@matchAllMethodIndicesForEach
                    }
                    val receiver = getInstruction(index).firstArgumentRegister
                    replaceInstruction(index, "invoke-static/range {v$receiver .. v$receiver}, $bridge")
                }
            }
        }
        addCommentAuthorIdentifier()

        // PostHistory has no spare column for the identifier and adding one requires
        // a Realm migration, so fold it into the nickname that is stored.
        val nameSetter = PostHistoryWriterFingerprint.method.run {
            val index = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.let { reference ->
                    reference.definingClass == POST_INFO_CLASS && reference.name in nameGetters
                } == true
            }

            val setter = getInstruction(index + 2).getReference<MethodReference>()
                ?: throw PatchException("Could not find the author name setter")
            val postRegister = getInstruction(index).firstArgumentRegister
            val nameRegister = getInstruction<OneRegisterInstruction>(index + 1).registerA
            val freeRegisters =
                getFreeRegisterProvider(index + 2, 2, postRegister, nameRegister)
            val userIdRegister = freeRegisters.getFreeRegister4Bit()
            val ipRegister = freeRegisters.getFreeRegister4Bit()

            addInstructions(
                index + 2,
                """
                    invoke-virtual {v$postRegister}, ${userIdGetter.smaliReference}
                    move-result-object v$userIdRegister
                    invoke-virtual {v$postRegister}, ${ipGetter.smaliReference}
                    move-result-object v$ipRegister
                    invoke-static {v$nameRegister, v$userIdRegister, v$ipRegister}, $EXTENSION_CLASS->foldAuthorIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$nameRegister
                """.trimIndent(),
            )

            setter
        }

        val storedNameField = nameSetter.getMutableMethod().instructions
            .firstNotNullOf { it.getReference<FieldReference>() }
        val history = mutableClassDefBy(nameSetter.definingClass)
        val getter = history.methods.single { it.readsOnly(storedNameField) }
        val summary = PostHistorySummaryBindFingerprint.originalMethod
        val display = historyNameBridge(getter, "formatAuthorName")
        val nickname = historyNameBridge(getter, "stripAuthorIdentifier")
        methodCall(getter).matchAllMethodIndicesForEach { index ->
            if (name.startsWith("morphe_")) return@matchAllMethodIndicesForEach
            val receiver = getInstruction(index).firstArgumentRegister
            val target = if (definingClass == summary.definingClass && name == summary.name) display else nickname
            replaceInstruction(index, "invoke-static/range {v$receiver .. v$receiver}, $target")
        }
    }
}

private fun addCommentAuthorIdentifier() {
    val authorLine = CommentAuthorLineFingerprint.originalMethod
    val modelType = authorLine.parameterTypes.first().toString()
    val model = classDefBy(modelType)
    val userId = model.gettersOf("user_id").first()
    var count = 0
    model.gettersOf("name").forEach { getter ->
        val bridge = authorNameBridge(modelType, getter, userId, "formatCommentAuthorName")
        methodCall(getter).matchAllMethodIndicesForEach(requireMatches = false) { index ->
            if (name.startsWith("morphe_") || instructions.none {
                    it.getReference<MethodReference>()?.definingClass?.startsWith("Landroid/text/Spannable") == true
                }
            ) return@matchAllMethodIndicesForEach
            val receiver = getInstruction(index).firstArgumentRegister
            replaceInstruction(index, "invoke-static/range {v$receiver .. v$receiver}, $bridge")
            count++
        }
    }
    check(count > 0) { "Could not find comment author rendering calls" }
    CommentPatchIncludedFingerprint.method.returnEarly(true)
}

private fun authorNameBridge(
    modelType: String,
    getter: Method,
    userId: Method,
    formatter: String,
) = ImmutableMethod(
    modelType,
    "morphe_${formatter}_${getter.name}",
    listOf(ImmutableMethodParameter(modelType, null, null)),
    "Ljava/lang/String;",
    AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
    null,
    null,
    MutableMethodImplementation(2),
).toMutable().apply {
    addInstructions(
        0,
        """
            invoke-virtual {p0}, ${getter.smaliReference}
            move-result-object v0
            invoke-virtual {p0}, ${userId.smaliReference}
            move-result-object p0
            invoke-static {v0, p0}, $EXTENSION_CLASS->$formatter(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
            move-result-object v0
            return-object v0
        """,
    )
    mutableClassDefBy(modelType).methods.add(this)
}

private fun historyNameBridge(getter: Method, formatter: String) = ImmutableMethod(
    getter.definingClass,
    "morphe_$formatter",
    listOf(ImmutableMethodParameter(getter.definingClass, null, null)),
    "Ljava/lang/String;",
    AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
    null,
    null,
    MutableMethodImplementation(1),
).toMutable().apply {
    addInstructions(
        0,
        """
            invoke-virtual {p0}, ${getter.smaliReference}
            move-result-object p0
            invoke-static {p0}, $EXTENSION_CLASS->$formatter(Ljava/lang/String;)Ljava/lang/String;
            move-result-object p0
            return-object p0
        """,
    )
    mutableClassDefBy(getter.definingClass).methods.add(this)
}

private val Instruction.firstArgumentRegister: Int
    get() = when (this) {
        is FiveRegisterInstruction -> registerC
        is RegisterRangeInstruction -> startRegister
        else -> throw PatchException("Expected a method invocation")
    }

// Matches a getter, and not methods such as 'toString' that read every field.
private fun ClassDef.gettersOf(serializedName: String): List<Method> {
    val field = fields.first { field ->
        field.annotations.any { annotation ->
            annotation.elements.any { element ->
                element.name == "value" &&
                    (element.value as? StringEncodedValue)?.value == serializedName
            }
        }
    }

    return methods.filter { method -> method.readsOnly(field) }
}

private fun Method.readsOnly(field: FieldReference) =
    parameterTypes.isEmpty() &&
        returnType == field.type &&
        !AccessFlags.STATIC.isSet(accessFlags) &&
        implementation?.instructions
            ?.mapNotNull { instruction -> instruction.getReference<FieldReference>() }
            ?.singleOrNull()?.name == field.name
