package app.revanced.patches.kakaotalk.emoticon

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.emoticon.fingerprints.emoticonPlusMeResultConstructorFingerprint

@Suppress("unused")
val forceEnableEmoticonPlusPatch = bytecodePatch(
    name = "Force enable emoticon plus feature",
    description = "Force enable emoticon plus feature (Unpurchased emoticon can be sent once per day)",
) {
    compatibleWith("com.kakao.talk"("26.1.2"))

    execute {
        emoticonPlusMeResultConstructorFingerprint.method.addInstruction(
            1,
            "const/4 p1, 0x1"
        )
    }
}