package app.revanced.patches.kakaotalk.interaction.send

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.getReference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.interaction.send.fingerprints.SendButtonClickFingerprint
import app.revanced.patches.kakaotalk.interaction.send.fingerprints.SendButtonLongClickFingerprint
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/EnableSendBigTextPatch;"
private const val ENABLE_SEND_BIG_TEXT =
    "Lapp/revanced/extension/kakaotalk/settings/Settings;->enableSendBigText()Z"
private const val SEND_BIG_TEXT_METHOD = "revanced_sendBigText"

@Suppress("unused")
val enableSendBigTextPatch = bytecodePatch(
    name = "Enable send big text",
    description = "Allows sending big text messages in KakaoTalk.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_enable_send_big_text",
                titleKey = "morphe_settings_patch_send_big_text",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val sendMethod = SendButtonClickFingerprint.method.instructions
            .mapNotNull { it.getReference<MethodReference>() }
            .singleOrNull { reference ->
                reference.parameterTypes.isEmpty() && reference.returnType == "V"
            }
            ?: throw PatchException("Could not resolve the send button click callback.")

        SendButtonLongClickFingerprint.method.apply {
            val longClickMethod = instructions
                .mapNotNull { it.getReference<MethodReference>() }
                .singleOrNull { reference ->
                    reference.parameterTypes.isEmpty() && reference.returnType == "Z"
                }
                ?: throw PatchException("Could not resolve the send button long click callback.")
            val listenerField = instructions
                .mapNotNull { it.getReference<FieldReference>() }
                .singleOrNull { it.type == sendMethod.definingClass }
                ?: throw PatchException("Could not resolve the send button listener field.")
            val unitField = instructions.firstNotNullOfOrNull { instruction ->
                instruction.takeIf { it.opcode == Opcode.SGET_OBJECT }
                    ?.getReference<FieldReference>()
                    ?.takeIf { it.type == "Lkotlin/Unit;" }
            }
                ?: throw PatchException("Could not resolve the long click return path.")

            SendButtonLongClickFingerprint.classDef.methods.add(
                sendBigTextMethod(
                    definingClass,
                    listenerField,
                    sendMethod,
                    longClickMethod,
                    unitField,
                ),
            )
            removeInstructions(0, instructions.size)
            addInstructions(
                0,
                """
                    invoke-static {p0}, $definingClass->$SEND_BIG_TEXT_METHOD($definingClass)Lkotlin/Unit;
                    move-result-object p0
                    return-object p0
                """.trimIndent(),
            )
        }
    }
}

private fun sendBigTextMethod(
    definingClass: String,
    listenerField: FieldReference,
    sendMethod: MethodReference,
    longClickMethod: MethodReference,
    unitField: FieldReference,
): MutableMethod = ImmutableMethod(
    definingClass,
    SEND_BIG_TEXT_METHOD,
    listOf(ImmutableMethodParameter(definingClass, null, null)),
    "Lkotlin/Unit;",
    AccessFlags.PRIVATE.value or AccessFlags.STATIC.value or AccessFlags.FINAL.value,
    null,
    null,
    MutableMethodImplementation(4),
).toMutable().apply {
    addInstructions(
        0,
        """
            invoke-static {}, $ENABLE_SEND_BIG_TEXT
            move-result v0
            if-eqz v0, :morphe_original
            invoke-virtual {p0}, $definingClass->getMessageEditText()Lcom/kakao/talk/activity/chatroom/inputbox/view/ChatRoomEditText;
            move-result-object v1
            invoke-virtual {v1}, Landroid/widget/TextView;->getEditableText()Landroid/text/Editable;
            move-result-object v1
            invoke-interface {v1}, Ljava/lang/CharSequence;->length()I
            move-result v0
            const/4 v2, 0x1
            if-gt v0, v2, :morphe_original
            const-string v0, "\u200d"
            invoke-interface {v1, v0}, Landroid/text/Editable;->append(Ljava/lang/CharSequence;)Landroid/text/Editable;
            iget-object v2, p0, ${listenerField.smaliReference}
            if-eqz v2, :morphe_return
            invoke-interface {v2}, ${sendMethod.smaliReference}
            goto :morphe_return
            :morphe_original
            iget-object v2, p0, ${listenerField.smaliReference}
            if-eqz v2, :morphe_return
            invoke-interface {v2}, ${longClickMethod.smaliReference}
            :morphe_return
            sget-object v0, ${unitField.smaliReference}
            return-object v0
        """.trimIndent(),
    )
}
