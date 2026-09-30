package app.revanced.patches.kakaotalk.layout.profile

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.misc.extension.addExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.addKakaoTalkResources
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/revanced/extension/kakaotalk/patches/CustomProfileImagePatch;"

@Suppress("unused")
val allowCustomProfileImagePatch = bytecodePatch(
    name = "Allow custom profile images",
    description = "Adds a photo picker and adjustable images to the custom profile editor.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch, addResourcesPatch)

    execute {
        addKakaoTalkResources()
        CustomProfileBitmapFingerprint.method

        CustomProfileCreateFingerprint.method.apply {
            val superIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_SUPER && getReference<MethodReference>()?.name == "onCreate"
            }
            addInstruction(
                superIndex + 1,
                "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->initialize(Landroid/app/Activity;)V",
            )
        }
        CustomProfileMenuFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p1 }, $EXTENSION_CLASS->addMenu(Landroid/app/Activity;Landroid/view/Menu;)V",
        )
        CustomProfileColorFingerprint.instructionMatches.single().let { match ->
            val call = match.getInstruction<FiveRegisterInstruction>()
            CustomProfileColorFingerprint.method.replaceInstruction(
                match.index,
                "invoke-static { v${call.registerC}, v${call.registerD} }, $EXTENSION_CLASS->setBackgroundColor(Landroid/view/View;I)V",
            )
        }
    }
}