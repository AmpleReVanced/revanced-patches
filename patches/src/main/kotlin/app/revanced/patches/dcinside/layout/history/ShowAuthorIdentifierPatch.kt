package app.revanced.patches.dcinside.layout.history

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getMutableMethod
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchAllMethodIndicesForEach
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
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
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import java.util.logging.Logger

private const val loggerName = "app.revanced.patches.dcinside.layout.history.ShowAuthorIdentifierPatch"

@Suppress("unused")
val showAuthorIdentifierPatch = bytecodePatch(
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

        val authorSpanBuilder = PostAuthorLineFingerprint.method.instructions.firstNotNullOf { instruction ->
            instruction.getReference<MethodReference>()
                ?.takeIf { reference -> reference.returnType == "Landroid/text/Spannable;" }
        }

        // Posts and post lists render the author line in obfuscated classes, so every method that
        // builds one is patched instead of fingerprinting each of them.
        listOf(POST_INFO_CLASS, POST_ITEM_CLASS).forEach { modelType ->
            val model = classDefBy(modelType)
            val modelUserIdGetter = model.gettersOf("user_id").first()

            model.gettersOf("name").forEach { nicknameGetter ->
                methodCall(
                    definingClass = modelType,
                    name = nicknameGetter.name,
                    parameters = emptyList(),
                    returnType = "Ljava/lang/String;",
                ).matchAllMethodIndicesForEach(requireMatches = false) { index ->
                    val buildsAuthorLine = instructions.any { instruction ->
                        instruction.getReference<MethodReference>()?.smaliReference ==
                            authorSpanBuilder.smaliReference
                    }
                    if (!buildsAuthorLine ||
                        getInstruction(index + 1).opcode != Opcode.MOVE_RESULT_OBJECT
                    ) {
                        return@matchAllMethodIndicesForEach
                    }

                    val modelRegister = getInstruction<FiveRegisterInstruction>(index).registerC
                    val nameRegister = getInstruction<OneRegisterInstruction>(index + 1).registerA
                    val userIdRegister =
                        getFreeRegisterProvider(index + 2, 1, modelRegister, nameRegister)
                            .getFreeRegister4Bit()

                    addInstructions(
                        index + 2,
                        """
                            invoke-virtual {v$modelRegister}, ${modelUserIdGetter.smaliReference}
                            move-result-object v$userIdRegister
                            invoke-static {v$nameRegister, v$userIdRegister}, $EXTENSION_CLASS->formatPostAuthorName(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$nameRegister
                        """.trimIndent(),
                    )
                }
            }
        }

        // Isolated, so that a comment side app change cannot drop the post identifiers as well.
        // On failure the comment preference stays hidden instead of becoming a dead toggle.
        try {
            addCommentAuthorIdentifier()
        } catch (exception: Exception) {
            Logger.getLogger(loggerName).warning(
                "Could not show the author identifier in comments: $exception",
            )
        }

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
            val postRegister = getInstruction<FiveRegisterInstruction>(index).registerC
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

        // The setter delegates to the accessor the Realm proxy overrides.
        val storedNameField = nameSetter.getMutableMethod().instructions
            .firstNotNullOf { instruction ->
                instruction.getReference<MethodReference>()?.takeIf { reference ->
                    reference.definingClass == nameSetter.definingClass &&
                        reference.parameterTypeNames == nameSetter.parameterTypeNames
                }
            }
            .getMutableMethod().instructions
            .firstNotNullOf { instruction -> instruction.getReference<FieldReference>() }

        val postHistory = mutableClassDefBy(nameSetter.definingClass)
        val storedName = postHistory.methods.first { method -> method.readsOnly(storedNameField) }
        val nameGetter = postHistory.methods.first { method ->
            method.parameterTypes.isEmpty() &&
                method.instructions.any { instruction ->
                    instruction.getReference<MethodReference>()?.smaliReference == storedName.smaliReference
                }
        }

        // Recently viewed posts are copied into the post archive and post series tables,
        // so the identifier is stripped in the getter every consumer reads.
        nameGetter.apply {
            val nicknameRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructions(
                0,
                """
                    invoke-virtual {p0}, ${storedName.smaliReference}
                    move-result-object v$nicknameRegister
                    invoke-static {v$nicknameRegister}, $EXTENSION_CLASS->stripAuthorIdentifier(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$nicknameRegister
                    return-object v$nicknameRegister
                """.trimIndent(),
            )
        }

        // Only the recently viewed list reads the value as stored, to show the identifier.
        PostHistorySummaryBindFingerprint.method.apply {
            val index = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.smaliReference == nameGetter.smaliReference
            }
            val recordRegister = getInstruction<FiveRegisterInstruction>(index).registerC
            val nicknameRegister = getInstruction<OneRegisterInstruction>(index + 1).registerA

            addInstructions(
                index + 2,
                """
                    invoke-virtual {v$recordRegister}, ${storedName.smaliReference}
                    move-result-object v$nicknameRegister
                    invoke-static {v$nicknameRegister}, $EXTENSION_CLASS->formatAuthorName(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$nicknameRegister
                """.trimIndent(),
            )
        }
    }
}

private fun BytecodePatchContext.addCommentAuthorIdentifier() {
    // Matched exhaustively, as the signature is what identifies the builder and a second
    // method of the same shape must not silently decide the type the bridge casts to.
    val commentAuthorLine = CommentAuthorLineFingerprint.matchSingle().originalMethod
    val commentType = commentAuthorLine.parameterTypes.first().toString()
    val commentModel = classDefBy(commentType)
    val commentUserIdGetter = commentModel.gettersOf("user_id").first()

    fun formatAuthorLine(authorLineRegister: Int, commentRegister: Int) =
        """
            invoke-static {v$authorLineRegister, v$commentRegister}, $EXTENSION_CLASS->formatCommentAuthorLine(Ljava/lang/CharSequence;Ljava/lang/Object;)Ljava/lang/CharSequence;
            move-result-object v$authorLineRegister
        """.trimIndent()

    var authorLineCallSites = 0
    val injections = mutableListOf<() -> Unit>()
    methodCall(commentAuthorLine).matchAllMethodIndicesForEach { index ->
        authorLineCallSites++
        if (getInstruction(index + 1).opcode != Opcode.MOVE_RESULT_OBJECT) {
            return@matchAllMethodIndicesForEach
        }

        val smaliInstructions = formatAuthorLine(
            getInstruction<OneRegisterInstruction>(index + 1).registerA,
            getInstruction<FiveRegisterInstruction>(index).registerD,
        )
        injections += { addInstructions(index + 2, smaliInstructions) }
    }

    if (injections.size != authorLineCallSites) {
        throw PatchException(
            "Only ${injections.size} of $authorLineCallSites comment author line call sites are patchable",
        )
    }

    CommentUserIdBridgeFingerprint.method.addInstructions(
        0,
        """
            check-cast p0, $commentType
            invoke-virtual {p0}, ${commentUserIdGetter.smaliReference}
            move-result-object p0
            return-object p0
        """.trimIndent(),
    )
    injections.forEach { injection -> injection() }
    CommentPatchIncludedFingerprint.method.returnEarly(true)

    // The capture screen builds the author line itself instead of calling the builder.
    var patchedCaptures = 0
    commentModel.gettersOf("name").forEach { nicknameGetter ->
        methodCall(
            definingClass = commentType,
            name = nicknameGetter.name,
            parameters = emptyList(),
            returnType = "Ljava/lang/String;",
        ).matchAllMethodIndicesForEach(requireMatches = false) { index ->
            val spannableIndex = indexOfFirstInstruction(
                index,
                methodCall(returnType = "Landroid/text/Spannable;"),
            )
            if (spannableIndex < 0 ||
                getInstruction(spannableIndex + 1).opcode != Opcode.MOVE_RESULT_OBJECT
            ) {
                return@matchAllMethodIndicesForEach
            }

            val authorLineRegister =
                getInstruction<OneRegisterInstruction>(spannableIndex + 1).registerA
            val consumer = getInstruction(spannableIndex + 2)
            if (consumer.getReference<MethodReference>()?.name != "setText" ||
                (consumer as? FiveRegisterInstruction)?.registerD != authorLineRegister
            ) {
                return@matchAllMethodIndicesForEach
            }

            addInstructions(
                spannableIndex + 2,
                formatAuthorLine(
                    authorLineRegister,
                    getInstruction<FiveRegisterInstruction>(index).registerC,
                ),
            )
            patchedCaptures++
        }
    }

    if (patchedCaptures == 0) {
        Logger.getLogger(loggerName).warning(
            "Could not patch the captured comment author line, " +
                "the author identifier will not be shown in comment captures.",
        )
    }
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
