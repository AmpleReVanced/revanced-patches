package app.revanced.patches.kakaotalk.changemodel

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.kakaotalk.changemodel.fingerprints.changeModelFingerprint

@Suppress("unused")
val changeModelPatch = bytecodePatch(
    name = "Change model",
    description = "Changes the device model to supporting subdevice features",
) {
    val changeModelOption by stringOption(
        key = "model",
        default = "SM-X926N",
        title = "Model",
        description = "Device model to change to (Only works for models that support subdevice features like SM-X926N)",
    )

    compatibleWith("com.kakao.talk"("25.8.2"))

    execute {
        changeModelFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "$changeModelOption"
                return-object v0
            """.trimIndent()
        )
    }
}