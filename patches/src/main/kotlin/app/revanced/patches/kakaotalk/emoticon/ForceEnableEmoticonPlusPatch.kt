package app.revanced.patches.kakaotalk.emoticon

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val forceEnableEmoticonPlusPatch = bytecodePatch(
    name = "Force enable emoticon plus feature",
    description = "Force enable emoticon plus feature (Unpurchased emoticon can be sent once per day)",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        EmoticonPlusMeResultConstructorFingerprint.method.addInstruction(
            1,
            "const/4 p1, 0x1"
        )
    }
}