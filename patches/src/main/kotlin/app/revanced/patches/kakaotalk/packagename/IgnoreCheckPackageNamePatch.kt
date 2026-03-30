package app.revanced.patches.kakaotalk.packagename

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import app.revanced.patches.all.misc.packagename.changePackageNamePatch
import app.revanced.patches.all.misc.packagename.packageNameOption
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference

@Suppress("unused")
val ignoreCheckPackageNamePatch = bytecodePatch(
    name = "Ignore Check Package Name",
    description = "Ignores the package name check to allow installation of modified versions.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    dependsOn(changePackageNamePatch)

    execute {
        CheckPackageNameFingerprint.method.returnEarly()

        GetInstallSourceInfoFingerprint.method.apply {
            val replacementPackageName = packageNameOption.value
            val newPackageName = if (replacementPackageName != packageNameOption.default) {
                replacementPackageName!!
            } else {
                "com.kakao.talk.revanced"
            }

            val packageName = instructions
                .filterIsInstance<BuilderInstruction21c>()
                .first { inst ->
                    inst.opcode == Opcode.CONST_STRING && inst.getReference<StringReference>()?.string == "com.kakao.talk"
                }

            replaceInstruction(
                packageName.location.index,
                BuilderInstruction21c(
                    Opcode.CONST_STRING,
                    packageName.registerA,
                    ImmutableStringReference(newPackageName)
                )
            )
        }
    }
}