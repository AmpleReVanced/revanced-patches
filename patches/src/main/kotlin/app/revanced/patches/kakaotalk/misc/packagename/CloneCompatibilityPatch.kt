package app.revanced.patches.kakaotalk.misc.packagename

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.cloneMutable
import app.revanced.patches.kakaotalk.misc.integrity.hookPackageManagerPatch
import app.revanced.patches.shared.misc.packagename.resolvePackageName
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val ORIGINAL_PACKAGE_NAME = "com.kakao.talk"
private const val EXTENSION_CLASS = "Lapp/revanced/extension/kakaotalk/patches/ChangePackageNamePatch;"

internal val cloneCompatibilityPatch = bytecodePatch(
    description = "Keeps cloned installs compatible with SDK requests, payment services and internal links.",
) {
    dependsOn(hookPackageManagerPatch, cloneTelemetryPatch)

    execute {
        KakaoSdkAgentFingerprint.method.addInstruction(0, "const-string p5, \"$ORIGINAL_PACKAGE_NAME\"")

        (listOf(
            KakaoSdkExtrasFingerprint.match(),
            PayAuthTalkPackageFingerprint.match(),
            KakaoTvApplicationPackageFingerprint.match(),
            FirebaseModelHeadersFingerprint.match(),
            PlacesGrpcHeadersFingerprint.match(),
            PlacesHttpHeadersFingerprint.match(),
        ) + PayPlatformPackageFingerprint.matchAll(2..2)).forEach { match ->
            val instruction = match.instructionMatches.last()
            val register = instruction.getInstruction<OneRegisterInstruction>().registerA
            match.method.replaceInstruction(instruction.index, "const-string v$register, \"$ORIGINAL_PACKAGE_NAME\"")
        }

        FirebaseInstallationHeadersFingerprint.apply {
            val instruction = instructionMatches.last()
            val register = instruction.getInstruction<FiveRegisterInstruction>().registerE
            method.addInstruction(instruction.index, "const-string v$register, \"$ORIGINAL_PACKAGE_NAME\"")
        }

        FidoDiagnosticPropertiesFingerprint.apply {
            val instruction = instructionMatches.single()
            val register = instruction.getInstruction<FiveRegisterInstruction>().registerC
            method.addInstruction(
                instruction.index,
                "invoke-static {v$register}, $EXTENSION_CLASS->sanitizeFidoProperties(Lorg/json/JSONObject;)V",
            )
        }

        FaceprintInitFingerprint.apply {
            val instruction = instructionMatches.single()
            val nativeInit = instruction.getMethodCalled()
            if (!AccessFlags.NATIVE.isSet(nativeInit.accessFlags)) {
                throw PatchException("The Faceprint initializer is not native.")
            }
            val contextInit = nativeInit.cloneMutable(
                parameters = listOf(ImmutableMethodParameter("Landroid/content/Context;", null, null)) + nativeInit.parameters.drop(1),
            )
            mutableClassDefBy(nativeInit.definingClass).methods.add(contextInit)

            val range = instruction.getInstruction<RegisterRangeInstruction>()
            val register = range.startRegister + 1
            method.replaceInstruction(
                instruction.index,
                "invoke-virtual/range {v${range.startRegister} .. v${range.startRegister + range.registerCount - 1}}, $contextInit",
            )
            method.addInstructions(
                instruction.index,
                """
                    invoke-static/range {v$register .. v$register}, $EXTENSION_CLASS->wrapContext(Landroid/content/Context;)Landroid/content/Context;
                    move-result-object v$register
                """.trimIndent(),
            )
        }

        KakaoTvTalkLinkFingerprint.apply {
            val instruction = instructionMatches.first()
            val register = instruction.getInstruction<OneRegisterInstruction>().registerA
            val packageName = resolvePackageName(packageNameOption, ORIGINAL_PACKAGE_NAME)
            method.replaceInstruction(instruction.index, "const-string v$register, \"$packageName\"")
        }
    }
}