package app.revanced.patches.kakaotalk.misc.integrity

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.AttestationPayloadFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val ORIGINAL_PACKAGE_NAME = "com.kakao.talk"

@Suppress("unused")
val spoofAttestationPackageNamePatch = bytecodePatch(
    name = "Spoof attestation package name",
    description = "Reports the original package name in the attestation report, which otherwise " +
            "reveals a renamed install. Only the report is affected, so the app keeps using its " +
            "real package name everywhere else.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        AttestationPayloadFingerprint.method.apply {
            val index = indexOfFirstInstructionOrThrow(
                methodCall("Landroid/content/Context;->getPackageName()Ljava/lang/String;"),
            )

            val resultIndex = index + 1
            if (getInstruction(resultIndex).opcode != Opcode.MOVE_RESULT_OBJECT) {
                throw PatchException("The attestation package name read is not captured.")
            }
            val register = getInstruction<OneRegisterInstruction>(resultIndex).registerA

            replaceInstructions(
                index,
                """
                    nop
                    const-string v$register, "$ORIGINAL_PACKAGE_NAME"
                """.trimIndent(),
            )
        }
    }
}
