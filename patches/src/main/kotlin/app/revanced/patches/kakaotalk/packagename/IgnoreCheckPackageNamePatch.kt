package app.revanced.patches.kakaotalk.packagename

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.packagename.changePackageNamePatch
import app.revanced.patches.kakaotalk.packagename.fingerprints.checkPackageNameFingerprint

@Suppress("unused")
val ignoreCheckPackageNamePatch = bytecodePatch(
    name = "Ignore Check Package Name",
    description = "Ignores the package name check to allow installation of modified versions.",
    use = false,
) {
    dependsOn(changePackageNamePatch)

    execute {
        checkPackageNameFingerprint.method.replaceInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )
    }
}