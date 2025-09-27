package app.revanced.patches.kakaotalk.member

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.member.fingerprints.alwaysShowKickButtonFingerprint
import app.revanced.patches.kakaotalk.member.fingerprints.containsUserByIdFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val alwaysShowKickButtonPatch = bytecodePatch(
    name = "Always Show Kick Button",
    description = "Always shows the kick button in group member management.",
) {
    compatibleWith("com.kakao.talk"("25.8.2"))

    execute {
        val containsUserByIdMethod = containsUserByIdFingerprint.method
        val alwaysShowKickButtonMethod = alwaysShowKickButtonFingerprint.method

        alwaysShowKickButtonMethod.instructions.indexOfFirst {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                    it.getReference<MethodReference>()?.name == containsUserByIdMethod.name &&
                    it.getReference<MethodReference>()?.definingClass == containsUserByIdMethod.definingClass
        }.let {
            val moveResultInst = alwaysShowKickButtonMethod.instructions.getOrNull(it + 1) as BuilderInstruction11x
            val register = moveResultInst.registerA

            alwaysShowKickButtonMethod.addInstructions(
                it + 2,
                """
                    const/4 v$register, 0x1
                """.trimIndent()
            )
        }
    }
}