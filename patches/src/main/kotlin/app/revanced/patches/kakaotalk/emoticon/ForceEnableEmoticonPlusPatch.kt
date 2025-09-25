package app.revanced.patches.kakaotalk.emoticon

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.emoticon.fingerprints.isEnableEmoticonPlusFingerprint

@Suppress("unused")
val forceEnableEmoticonPlusPatch = bytecodePatch(
    name = "Force enable emoticon plus feature",
    description = "Force enable emoticon plus feature (Unpurchased emoticon can be sent once per day)",
) {
    compatibleWith("com.kakao.talk"("25.8.1"))

    execute {
        isEnableEmoticonPlusFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )
    }
}