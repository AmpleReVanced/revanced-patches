package app.revanced.patches.kakaotalk.misc.spoof.versioninfo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("unused")
val versionInfoPatch = patch(
    name = "Version info patch",
    description = "Patches the version info to include '(ReVanced)' in the version string.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val label = "${packageMetadata.versionName} (ReVanced)"
        val builtAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        VersionInfoFingerprint.replaceVersionLabel("$label\nBuild at: $builtAt")
        VersionInfoPreviewFingerprint.replaceVersionLabel(label)
        AdaptiveVersionInfoFingerprint.replaceVersionLabel(label)

        AdaptiveVersionInfoPreviewFingerprint.apply {
            val constructor = instructionMatches.last()
            val register = constructor.getInstruction<FiveRegisterInstruction>().registerD
            method.addInstruction(constructor.index, "const-string v$register, \"$label\"")
        }
    }
}

private fun Fingerprint.replaceVersionLabel(label: String) {
    val index = instructionMatches[1].index
    val register = method.getInstruction<OneRegisterInstruction>(index).registerA
    method.replaceInstruction(index, BuilderInstruction21c(Opcode.CONST_STRING, register, ImmutableStringReference(label)))
}