package app.revanced.patches.kakaotalk.misc.feature

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/DisableOpenChatRoomCommentPatch;"

private object OpenChatCommentSettingFingerprint : Fingerprint(
    parameters = listOf("L"),
    returnType = "Z",
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L"), returnType = "Z"),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = listOf("J", "L"), returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatThreadFeatureFlagManager.kt" },
)

@Suppress("unused")
val disableOpenChatRoomCommentPatch = patch(
    name = "Disable open chat room comments",
    description = "Adds a setting to disable comments in open chat rooms.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_open_chat_room_comment_disabled",
                titleKey = "morphe_settings_patch_open_chat_room_comment_disabled",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val fingerprint = OpenChatCommentSettingFingerprint
        val index = fingerprint.instructionMatches.last().index
        val register = fingerprint.method.getInstruction<OneRegisterInstruction>(index).registerA
        fingerprint.method.addInstructions(
            index + 1,
            """
                invoke-static/range {v$register .. v$register}, $EXTENSION_CLASS->commentsDisabled(Z)Z
                move-result v$register
            """,
        )
    }
}
