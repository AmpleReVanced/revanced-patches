package app.revanced.patches.kakaotalk.misc.spoof.versioninfo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import java.time.format.DateTimeFormatter

@Suppress("unused")
val versionInfoPatch = bytecodePatch(
    name = "Version info patch",
    description = "Patches the version info to include '(ReVanced)' in the version string.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val versionPattern = Regex("^\\d+\\.\\d+\\.\\d+")
        val runPatch: (Fingerprint, Boolean) -> Unit = { fp, inDetail ->
            val versionInstruction = fp.method.instructions
                .filterIsInstance<BuilderInstruction21c>()
                .first { instruction ->
                    instruction.opcode == Opcode.CONST_STRING &&
                        versionPattern.containsMatchIn(
                            (instruction.reference as? StringReference)?.string.orEmpty()
                        )
                }
            val versionString = (versionInstruction.reference as StringReference).string

            fp.method
                .replaceInstruction(
                    versionInstruction.location.index,
                    BuilderInstruction21c(
                        Opcode.CONST_STRING,
                        versionInstruction.registerA,
                        ImmutableStringReference(
                            if (inDetail) {
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                "$versionString (ReVanced)\nBuild at: ${
                                    formatter.format(
                                        java.time.LocalDateTime.now()
                                    )
                                }"
                            } else {
                                "$versionString (ReVanced)"
                            }
                        )
                    )
                )
        }

        runPatch(VersionInfoFingerprint, true)
        runPatch(VersionInfoPreviewFingerprint, false)
    }
}