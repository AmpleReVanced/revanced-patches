package app.revanced.patches.kakaotalk.versioninfo

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.versioninfo.fingerprints.versionInfoFingerprint
import app.revanced.patches.kakaotalk.versioninfo.fingerprints.versionInfoPreviewFingerprint
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
    compatibleWith("com.kakao.talk"("25.7.3"))

    execute {
        val runPatch: (Fingerprint, Boolean) -> Unit = { fp, inDetail ->
            val versionInfo = fp.method.instructions
                .filterIsInstance<BuilderInstruction21c>()
                .first { inst ->
                    inst.opcode == Opcode.CONST_STRING
                }

            val versionString = (versionInfo.reference as StringReference).string

            fp.method
                .replaceInstruction(
                    versionInfo.location.index,
                    BuilderInstruction21c(
                        Opcode.CONST_STRING,
                        versionInfo.registerA,
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

        runPatch(versionInfoFingerprint, true)
        runPatch(versionInfoPreviewFingerprint, false)
    }
}