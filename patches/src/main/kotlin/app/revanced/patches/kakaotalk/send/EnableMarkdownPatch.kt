package app.revanced.patches.kakaotalk.send

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.send.fingerprints.EnableMarkdownFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/EnableMarkdownPatch;"

@Suppress("unused")
val enableMarkdownPatch = bytecodePatch(
    name = "Enable Markdown",
    description = "Render plain text messages with markdown-style formatting.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            SwitchPreference(
                key = "morphe_pref_enable_markdown",
                titleKey = "morphe_settings_patch_markdown",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val method = EnableMarkdownFingerprint.method
        val jsonInvokeIndex = method.instructions.indexOfLast { instruction ->
            if (instruction.opcode != Opcode.INVOKE_STATIC) return@indexOfLast false

            val reference = instruction.getReference<MethodReference>() ?: return@indexOfLast false
            reference.returnType == "Lorg/json/JSONObject;" &&
                    reference.parameterTypes == listOf(
                        "Lorg/json/JSONObject;",
                        "Z"
                    )
        }
        if (jsonInvokeIndex < 0) {
            throw PatchException("Could not find markdown JSON builder call")
        }

        val jsonMoveResultIndex = jsonInvokeIndex + 1
        if (jsonMoveResultIndex >= method.instructions.size || method.instructions[jsonMoveResultIndex].opcode != Opcode.MOVE_RESULT_OBJECT) {
            throw PatchException("Could not find JSON move-result after markdown JSON builder call")
        }
        val jsonRegister = (method.getInstruction(jsonMoveResultIndex) as OneRegisterInstruction).registerA

        val inputTextReference = method.instructions
            .mapNotNull { instruction ->
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@mapNotNull null
                instruction.getReference<MethodReference>()
            }
            .firstOrNull {
                it.definingClass == method.parameterTypes.first() &&
                        it.returnType == "Ljava/lang/CharSequence;" &&
                        it.parameterTypes.isEmpty()
            } ?: throw PatchException("Could not find input text accessor")

        val freeRegisters = method.getFreeRegisterProvider(
            jsonMoveResultIndex + 1,
            2,
            jsonRegister
        )
        val jsonArgumentRegister = freeRegisters.getFreeRegister4Bit()
        val inputTextRegister = freeRegisters.getFreeRegister4Bit()

        method.addInstructions(
            jsonMoveResultIndex + 1,
            """
                move-object/from16 v$jsonArgumentRegister, v$jsonRegister
                move-object/from16 v$inputTextRegister, p1
                invoke-virtual {v$inputTextRegister}, $inputTextReference
                move-result-object v$inputTextRegister
                invoke-static {v$jsonArgumentRegister, v$inputTextRegister}, $EXTENSION_CLASS->enableMarkdown(Lorg/json/JSONObject;Ljava/lang/CharSequence;)V
            """.trimIndent()
        )
    }
}
