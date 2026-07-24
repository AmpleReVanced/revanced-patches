package app.revanced.patches.dcinside.history

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getMutableMethod
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.misc.addExtensionPatch
import app.revanced.patches.dcinside.settings.PreferenceScreen
import app.revanced.patches.dcinside.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.revanced.util.parameterTypeNames
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/PostHistoryAuthorIdentifierPatch;"

@Suppress("unused")
val showPostHistoryAuthorIdentifierPatch = bytecodePatch(
    name = "Show recent post author identifier",
    description = "Shows the author identifier next to the nickname in the recently-viewed posts " +
        "list. Only applies to posts opened after this patch is installed.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addExtensionPatch, addSettingsPatch)

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            SwitchPreference(
                key = "morphe_pref_show_post_history_author_identifier",
                titleKey = "morphe_settings_show_post_history_author_identifier",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val postInfo = classDefBy(POST_INFO_CLASS)
        fun serializedField(serializedName: String) = postInfo.fields.first { field ->
            field.annotations.any { annotation ->
                annotation.elements.any { element ->
                    element.name == "value" &&
                        (element.value as? StringEncodedValue)?.value == serializedName
                }
            }
        }

        val nameField = serializedField("name")
        val userIdField = serializedField("user_id")
        val ipField = serializedField("ip")

        val nameGetters = postInfo.methods
            .filter { method -> method.readsOnly(nameField) }
            .map { method -> method.name }
            .toSet()
        val userIdGetter = postInfo.methods.first { method -> method.readsOnly(userIdField) }
        val ipGetter = postInfo.methods.first { method -> method.readsOnly(ipField) }

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

// Matches a getter, and not methods such as 'toString' that read every field.
private fun Method.readsOnly(field: FieldReference) =
    parameterTypes.isEmpty() &&
        returnType == field.type &&
        !AccessFlags.STATIC.isSet(accessFlags) &&
        implementation?.instructions
            ?.mapNotNull { instruction -> instruction.getReference<FieldReference>() }
            ?.singleOrNull()?.name == field.name
