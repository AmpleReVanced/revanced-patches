package app.revanced.patches.kakaotalk.versioninfo

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
        val runPatch: (Fingerprint, Boolean) -> Unit = { fp, inDetail ->
            val versionInfo = fp.method.instructions
                .filterIsInstance<BuilderInstruction21c>()
                .filter { inst ->
                    inst.opcode == Opcode.CONST_STRING
                }

            val index = if (inDetail) 1 else 0

            val versionString = (versionInfo[index].reference as StringReference).string

            fp.method
                .replaceInstruction(
                    versionInfo[index].location.index,
                    BuilderInstruction21c(
                        Opcode.CONST_STRING,
                        versionInfo[index].registerA,
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