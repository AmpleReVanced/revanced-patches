package app.revanced.patches.kakaotalk.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tab.fingerprints.DetermineFeedOrListMethodFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.MainTabConfigFingerprint
import app.revanced.util.matches
import app.revanced.util.parameterTypeNames
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val disableFriendFeedTabPatch = bytecodePatch(
    name = "Disable Friend Feed tab",
    description = "Disables the Friend Feed tab in KakaoTalk.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        MainTabConfigFingerprint.method.addInstructions(
            MainTabConfigFingerprint.method.instructions.size - 1,
            """
                const/4 p1, 0x0
                iput-boolean p1, p0, ${MainTabConfigFingerprint.method.definingClass}->a:Z
            """.trimIndent()
        )

        val feedGateReference = DetermineFeedOrListMethodFingerprint.method.instructions
            .mapNotNull { it.getReference<MethodReference>() }
            .singleOrNull { reference ->
                reference.parameterTypeNames.isEmpty() && reference.returnType == "Z"
            }
            ?: throw PatchException("Could not resolve the friend feed gate call.")

        mutableClassDefBy(feedGateReference.definingClass).methods
            .singleOrNull(feedGateReference::matches)
            ?.returnEarly(false)
            ?: throw PatchException("Could not resolve the friend feed gate.")

        DetermineFeedOrListMethodFingerprint.method.apply {
            val stateIndex = instructions.indexOfFirst { instruction ->
                instruction.getReference<MethodReference>()?.let { reference ->
                    reference.name == "<init>" &&
                        reference.parameterTypeNames == listOf("Z", "Z", "Z")
                } == true
            }.takeIf { it >= 0 }
                ?: throw PatchException("Could not find the friend tab state constructor call.")

            val feedRegister = (getInstruction(stateIndex) as FiveRegisterInstruction).registerE
            addInstruction(stateIndex, "const/4 v$feedRegister, 0x0")
        }
    }
}