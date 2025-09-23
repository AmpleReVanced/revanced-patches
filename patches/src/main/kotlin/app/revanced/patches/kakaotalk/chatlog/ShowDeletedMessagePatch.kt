package app.revanced.patches.kakaotalk.chatlog

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatlog.fingerprints.chatLogGetTextFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.chatLogSetTextFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.replaceToFeedFingerprint
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val showDeletedMessagePatch = bytecodePatch(
    name = "Show deleted messages",
    description = "Allows you to see deleted messages in chat logs.",
    use = false // TODO: 삭제 방법이 바뀌어서 작동하지 않음, 수정 필요
) {
    compatibleWith("com.kakao.talk"("25.8.0"))

    execute {
        val method = replaceToFeedFingerprint.method
        val insns = method.instructions

        val orInsnsIdx = insns
            .indexOfFirst { it.opcode == Opcode.OR_INT_LIT16 }

        if (orInsnsIdx == -1) error("could not find or built message $orInsnsIdx")

        method.addInstructions(
            orInsnsIdx + 1,
            """
                const/16 p1, 0x1
            """.trimIndent()
        )

        val setTextMethod = chatLogSetTextFingerprint.method.name
        val getTextMethod = chatLogGetTextFingerprint.method.name
        val chatLogClass = chatLogSetTextFingerprint.method.definingClass

        method.replaceInstruction(
            orInsnsIdx + 2,
            "nop"
        )

        method.addInstructions(
            orInsnsIdx + 3,
            """
                invoke-virtual {p0}, $chatLogClass->$getTextMethod()Ljava/lang/String;
                move-result-object v6
                
                const-string v7, "[Deleted]\u200b "
                
                invoke-virtual {v6, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
                move-result v8
                
                if-nez v8, :skip_deleted_prefix
                
                new-instance v8, Ljava/lang/StringBuilder;
                invoke-direct {v8}, Ljava/lang/StringBuilder;-><init>()V
                invoke-virtual {v8, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                invoke-virtual {v8, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
                move-result-object v6
                
                invoke-virtual {p0, v6}, $chatLogClass->$setTextMethod(Ljava/lang/String;)V
                
                :skip_deleted_prefix
                invoke-virtual {p0}, $chatLogClass->getChatRoomId()J
                move-result-wide v2
            """.trimIndent()
        )
    }
}