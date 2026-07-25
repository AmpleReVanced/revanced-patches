package app.revanced.patches.kakaotalk.emoticon

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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

        UserPreferenceIsActiveEmoticonPlusFingerprint.method.returnEarly(true)

        val caller = EmoticonPlusManagerIsActiveFingerprint.method.instructions
            .first { it.opcode == Opcode.INVOKE_VIRTUAL }
            .getReference<MethodReference>()
        if (caller!!.definingClass != EMOTICON_PLUS_ME_RESULT_CLASS) {
            error("Force enable emoticon plus feature is not supported")
        }

        EmoticonPlusMeResultConstructorFingerprint.classDef.methods
            .first { it.name == caller.name }
            .returnEarly(true)
    }
}