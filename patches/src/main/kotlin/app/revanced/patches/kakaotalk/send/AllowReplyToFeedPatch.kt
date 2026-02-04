package app.revanced.patches.kakaotalk.send

import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.send.fingerprints.allowSwipeReplyToFeedFingerprint
import app.revanced.patches.kakaotalk.send.fingerprints.isCarouselTypeFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val allowReplyToFeedPatch = bytecodePatch(
    name = "Allow reply to feed",
    description = "Allows replying to feed messages",
) {
    compatibleWith("com.kakao.talk"("26.1.2"))

    execute {
        allowSwipeReplyToFeedFingerprint.method.apply {
            val getChatTypeInst = instructions.first { it.opcode == Opcode.INVOKE_VIRTUAL }
                .getReference<MethodReference>()
                ?: throw PatchException("Failed to find method reference for getting chat type")

            val getChatTypeMethodName = getChatTypeInst.name
            val getChatTypeClassName = getChatTypeInst.definingClass

            val chatType = instructions.first { it.opcode == Opcode.SGET_OBJECT }
                .getReference<FieldReference>()?.definingClass
                ?: throw PatchException("Failed to infer chat type enum class")

            val isCarouselTypeMethod = isCarouselTypeFingerprint.method

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
    }
}