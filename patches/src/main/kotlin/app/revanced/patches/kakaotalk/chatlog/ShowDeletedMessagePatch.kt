package app.revanced.patches.kakaotalk.chatlog

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.chatlog.fingerprints.chatLogGetTextFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.chatLogSetTextFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.replaceToFeedFingerprint
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val showDeletedMessagePatch = bytecodePatch(
    name = "Show deleted messages",
    description = "Allows you to see deleted messages in chat logs.",
    use = false
) {
    compatibleWith("com.kakao.talk"("25.8.0"))

    execute {
        val method = replaceToFeedFingerprint.method

        println("Patching method: ${method.name} in class ${method.definingClass}")

        val insns = method.instructions

        // pswitch(2,3) 블록에서 사용되는 플래그 세팅: or-int/lit16 p2, p2, 0x4000
        val orIdx = insns.indexOfFirst { it.opcode == Opcode.OR_INT_LIT16 }
        if (orIdx == -1) error("could not find OR_INT_LIT16 in method ${method.name}")

        val setTextMethod = chatLogSetTextFingerprint.method.name
        val getTextMethod = chatLogGetTextFingerprint.method.name
        val chatLogClass = chatLogSetTextFingerprint.method.definingClass

        /*
         * or-int/lit16 직전에 접두사 처리 로직을 주입하고, 마지막에 return-void로
         * 메서드를 종료합니다. 이렇게 하면 원래 플래그 설정 및 M1 호출은 실행되지 않습니다.
         *
         * 중요: 이 메서드의 시그니처 상 chatLog 파라미터는 p1 입니다.
         *       get/set 메서드는 p1(=chatLog)에 호출해야 합니다.
         *
         * 사용 레지스터: v0, v1, v2 (이 메서드의 .registers 14 환경에서 안전)
         */
        method.addInstructionsWithLabels(
            orIdx,
            """
                # String prefix = "[Deleted] \u200d"
                const-string v0, "[Deleted] \u200d"

                # String msg = chatLog.getMessage()
                invoke-virtual {p1}, $chatLogClass->$getTextMethod()Ljava/lang/String;
                move-result-object v1

                # if (msg.startsWith(prefix)) { chatLog.setMessage(prefix + msg); }
                invoke-virtual {v1, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
                move-result v2
                if-nez v2, :end_deleted_prefix

                new-instance v2, Ljava/lang/StringBuilder;
                invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
                invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
                move-result-object v0

                invoke-virtual {p1, v0}, $chatLogClass->$setTextMethod(Ljava/lang/String;)V
                
                const/4 v2, 0x1
                invoke-virtual {p1, v2}, $chatLogClass->b2(Z)V
                
                :end_deleted_prefix
                return-void
            """.trimIndent()
        )
    }
}
