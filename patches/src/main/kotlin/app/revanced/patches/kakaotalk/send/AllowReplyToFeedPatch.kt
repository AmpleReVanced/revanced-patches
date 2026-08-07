package app.revanced.patches.kakaotalk.send

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.send.fingerprints.AllowSwipeReplyToFeedFingerprint
import app.revanced.patches.kakaotalk.send.fingerprints.IsCarouselTypeFingerprint
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/AllowReplyToFeedPatch;"
private const val ALLOW_REPLY_TO_FEED =
    "Lapp/revanced/extension/kakaotalk/settings/Settings;->allowReplyToFeed()Z"

@Suppress("unused")
val allowReplyToFeedPatch = bytecodePatch(
    name = "Allow reply to feed",
    description = "Adds a setting to allow swiping feed messages to reply, or to comment where chat " +
            "room comments are enabled.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_allow_reply_to_feed",
                titleKey = "morphe_settings_patch_allow_reply_to_feed",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        AllowSwipeReplyToFeedFingerprint.method.apply {
            val getChatTypeInst = instructions.first { it.opcode == Opcode.INVOKE_VIRTUAL }
                .getReference<MethodReference>()
                ?: throw PatchException("Failed to find method reference for getting chat type")

            val getChatTypeMethodName = getChatTypeInst.name
            val getChatTypeClassName = getChatTypeInst.definingClass

            val chatType = instructions.first { it.opcode == Opcode.SGET_OBJECT }
                .getReference<FieldReference>()?.definingClass
                ?: throw PatchException("Failed to infer chat type enum class")

            val isCarouselTypeMethod = IsCarouselTypeFingerprint.method

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $ALLOW_REPLY_TO_FEED
                    move-result v0
                    if-eqz v0, :morphe_feed_original

                    invoke-virtual {p2}, $getChatTypeClassName->$getChatTypeMethodName()$chatType
                    move-result-object v0

                    sget-object v2, $chatType->Leverage:$chatType
                    if-ne v0, v2, :cond_not_leverage

                    instance-of v0, p2, ${isCarouselTypeMethod.definingClass}
                    if-eqz v0, :cond_not_leverage

                    move-object v0, p2
                    check-cast v0, ${isCarouselTypeMethod.definingClass}
                    invoke-virtual {v0}, ${isCarouselTypeMethod.definingClass}->${isCarouselTypeMethod.name}()Z
                    move-result v0
                    if-eqz v0, :cond_not_leverage

                    const/4 v0, 0x0
                    return v0

                    :cond_not_leverage
                    const/4 v0, 0x1
                    return v0

                    :morphe_feed_original
                    nop
                """.trimIndent()
            )
        }

        // Enabling comments adds a second swipe gate that rejects every feed type, so without
        // this the gesture is not registered at all and swiping does nothing.
        val (chatRoomType, chatLogType) = AllowSwipeReplyToFeedFingerprint.originalMethod
            .parameterTypes.map { parameterType -> parameterType.toString() }

        AllowSwipeReplyToFeedFingerprint.classDef.methods
            .first { method -> method.name == "getMovementFlags" }
            .apply {
                val index = indexOfFirstInstructionOrThrow(
                    methodCall(
                        definingClass = chatLogType,
                        parameters = listOf(chatRoomType),
                        returnType = "Z",
                    ),
                )
                if (getInstruction(index + 1).opcode != Opcode.MOVE_RESULT) {
                    throw PatchException("Comment swipe gate does not capture its result")
                }

                val register = getInstruction<OneRegisterInstruction>(index + 1).registerA
                val freeRegister = getFreeRegisterProvider(index + 2, 1, register).getFreeRegister4Bit()

                addInstructionsWithLabels(
                    index + 2,
                    """
                        invoke-static {}, $ALLOW_REPLY_TO_FEED
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :morphe_comment_original
                        const/4 v$register, 0x1
                        :morphe_comment_original
                        nop
                    """.trimIndent(),
                )
            }
    }
}