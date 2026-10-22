package app.revanced.patches.kakaotalk.misc.packagename

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.Option
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.misc.integrity.hookPackageManagerPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.shared.misc.packagename.baseChangePackageNamePatch
import app.revanced.patches.shared.misc.packagename.resolvePackageName
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference

lateinit var packageNameOption: Option<String>

@Suppress("unused")
private val ignoreCheckPackageNamePatch = bytecodePatch(
    description = "Ignores the package name check to allow installation of modified versions.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(hookPackageManagerPatch)

    execute {
        CheckPackageNameFingerprint.method.returnEarly()

        EmoticonDecryptContextFingerprint.let {
            it.method.apply {
                val index = it.instructionMatches.last().index
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index,
                    """
                        invoke-static {v$register}, Lapp/revanced/extension/kakaotalk/patches/ChangePackageNamePatch;->wrapContext(Landroid/content/Context;)Landroid/content/Context;
                        move-result-object v$register
                    """.trimIndent(),
                )
            }
        }

        GetInstallSourceInfoFingerprint.method.apply {
            val newPackageName = resolvePackageName(packageNameOption, "com.kakao.talk")

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

@Suppress("unused")
val changePackageNamePatch = bytecodePatch(
    name = "Change package name",
    description = "Appends \".revanced\" to the package name by default. " +
            "For KakaoTalk only",
    default = false,
) {
    packageNameOption = stringOption(
        key = "packageName",
        default = "Default",
        values = mapOf("Default" to "Default"),
        title = "Package name",
        description = "The name of the package to rename the app to.",
        required = true,
    ) {
        it == "Default" || it!!.matches(Regex("^[a-z]\\w*(\\.[a-z]\\w*)+\$"))
    }

    val updatePermissions by booleanOption(
        key = "updatePermissions",
        default = false,
        title = "Update permissions",
        description = "Update compatibility receiver permissions. " +
            "Enabling this can fix installation errors, but this can also break features in certain apps.",
    )

    val updateProviders by booleanOption(
        key = "updateProviders",
        default = false,
        title = "Update providers",
        description = "Update provider names declared by the app. " +
            "Enabling this can fix installation errors, but this can also break features in certain apps.",
    )

    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(
        baseChangePackageNamePatch(
            packageName = { packageNameOption },
            updatePermissions = { updatePermissions == true },
            updateProviders = { updateProviders == true },
        ),
        ignoreCheckPackageNamePatch,
        cloneCompatibilityPatch,
    )
}