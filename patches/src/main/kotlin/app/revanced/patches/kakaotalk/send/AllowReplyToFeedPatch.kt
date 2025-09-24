package app.revanced.patches.kakaotalk.send

import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.send.fingerprints.allowSwipeReplyToFeedFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val allowReplyToFeedPatch = bytecodePatch(
    name = "Allow reply to feed",
    description = "Allows replying to feed messages",
) {
    compatibleWith("com.kakao.talk"("25.8.0"))

    execute {
        allowSwipeReplyToFeedFingerprint.method.apply {
            val getChatTypeInst = instructions.first { it.opcode == Opcode.INVOKE_VIRTUAL }.getReference<MethodReference>()
                ?: throw PatchException("Failed to find method reference for getting chat type")
            val getChatTypeMethodName = getChatTypeInst.name
            val getChatTypeClassName = getChatTypeInst.definingClass
            val chatType = instructions.first { it.opcode == Opcode.SGET_OBJECT }.getReference<FieldReference>()?.definingClass

            replaceInstructions(
                0,
                """
                    invoke-virtual {p2}, $getChatTypeClassName->$getChatTypeMethodName()$chatType
                    move-result-object v0
                    sget-object v2, $chatType->Leverage:$chatType
                    if-ne v0, v2, :cond_0
                    const/4 v0, 0x0
                    return v0
                    :cond_0
                    const/4 v0, 0x1
                    return v0
                """.trimIndent()
            )
        }
    }
}