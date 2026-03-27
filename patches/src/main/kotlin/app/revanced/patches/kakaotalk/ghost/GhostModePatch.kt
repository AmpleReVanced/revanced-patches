package app.revanced.patches.kakaotalk.ghost

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val ghostMode = bytecodePatch(
    name = "Ghost Mode",
    description = "Don't expose your typing status to the other party.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val locoMethodClass = LocoMethodClassFingerprint.classDef
        val actionJobClass = actionJobMethodFingerprint(locoMethodClass).classDef
        val sendActionMethod = sendCurrentActionFingerprint(actionJobClass).method
        val protocolSuccessClass = ProtocolSuccessFingerprint.classDef

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