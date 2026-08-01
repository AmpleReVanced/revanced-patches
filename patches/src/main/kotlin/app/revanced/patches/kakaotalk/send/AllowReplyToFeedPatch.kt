package app.revanced.patches.kakaotalk.send

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.send.fingerprints.AllowSwipeReplyToFeedFingerprint
import app.revanced.patches.kakaotalk.send.fingerprints.IsCarouselTypeFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val allowReplyToFeedPatch = bytecodePatch(
    name = "Allow reply to feed",
    description = "Allows swiping feed messages to reply, or to comment where chat room comments are enabled.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
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

            replaceInstructions(
                0,
                """
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

                addInstructions(index + 2, "const/4 v$register, 0x1")
            }
    }
}