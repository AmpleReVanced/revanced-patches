package app.revanced.patches.kakaotalk.ghost

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ghost.fingerprints.actionJobMethodFingerprint
import app.revanced.patches.kakaotalk.ghost.fingerprints.locoMethodClassFingerprint
import app.revanced.patches.kakaotalk.ghost.fingerprints.protocolSuccessFingerprint
import app.revanced.patches.kakaotalk.ghost.fingerprints.sendCurrentActionFingerprint

@Suppress("unused")
val ghostMode = bytecodePatch(
    name = "Ghost Mode",
    description = "Don't expose your typing status to the other party.",
) {
    compatibleWith("com.kakao.talk"("25.8.3"))

    execute {
        val locoMethodClass = locoMethodClassFingerprint.classDef
        val actionJobClass = actionJobMethodFingerprint(locoMethodClass).classDef
        val sendActionMethod = sendCurrentActionFingerprint(actionJobClass).method
        val protocolSuccessClass = protocolSuccessFingerprint.classDef

        sendActionMethod.replaceInstructions(
            0,
            """
                const/4 v0, 0x0
                
                new-instance v1, ${protocolSuccessClass.type}
                
                invoke-direct {v1, v0}, ${protocolSuccessClass.type}-><init>(Ljava/lang/Object;)V
                
                return-object v1
            """
        )
    }
}