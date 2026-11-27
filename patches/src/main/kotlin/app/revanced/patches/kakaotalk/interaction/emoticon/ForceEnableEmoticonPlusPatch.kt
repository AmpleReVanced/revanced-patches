package app.revanced.patches.kakaotalk.interaction.emoticon

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val forceEnableEmoticonPlusPatch = patch(
    name = "Force enable emoticon plus feature",
    description = "Force enable emoticon plus feature (Unpurchased emoticon can be sent once per day)",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        EmoticonPlusMeResultConstructorFingerprint.method.addInstruction(
            1,
            "const/4 p1, 0x1"
        )

        EmoticonPlusMeResultIsActiveFingerprint.method.returnEarly(true)
    }
}