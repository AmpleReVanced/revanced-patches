package app.revanced.patches.kakaotalk.interaction.member

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.interaction.chatlog.bridgeMethod
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogItemViewHolderFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogViewHolderBindProfileFingerprint
import app.revanced.patches.kakaotalk.misc.extension.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.extension.sharedExtensionPatch
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.addKakaoTalkResources
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/revanced/extension/kakaotalk/patches/MemberMemoPatch;"
private const val BIND_NICKNAME_METHOD = "morphe_bindMemberMemo"

@Suppress("unused")
val memberMemoPatch = patch(
    name = "Open chat member memo",
    description = "Adds a private memo to open chat members, shown on their profile and next to their nickname.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch, addResourcesPatch, addSettingsTabPatch, sharedExtensionPatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_member_memo_nickname",
                titleKey = "morphe_settings_patch_member_memo_nickname",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)
        addKakaoTalkResources()

        listOf(OpenProfileViewerCreateFingerprint, OlkProfileCreateFingerprint).forEach {
            it.method.addInstruction(
                it.instructionMatches.first().index + 1,
                "invoke-static {p0}, $EXTENSION_CLASS->attachProfileMemo(Landroid/app/Activity;)V",
            )
        }

        val chatLogType = ChatLogFingerprint.classDef.type
        ChatLogViewHolderBindProfileFingerprint.let {
            val viewHolder = it.classDef.type
            it.classDef.methods.add(
                bridgeMethod(
                    definingClass = viewHolder,
                    name = BIND_NICKNAME_METHOD,
                    parameterTypes = listOf("Landroid/widget/TextView;"),
                    returnType = "V",
                    registerCount = 4,
                    instructions = """
                        invoke-virtual {p0}, ${ChatLogItemViewHolderFingerprint.method.smaliReference}
                        move-result-object v0
                        instance-of v1, v0, $chatLogType
                        if-eqz v1, :morphe_not_chat_log
                        check-cast v0, $chatLogType
                        invoke-virtual {v0}, $chatLogType->getUserId()J
                        move-result-wide v0
                        invoke-static {p1, v0, v1}, $EXTENSION_CLASS->bindNickname(Landroid/widget/TextView;J)V
                        :morphe_not_chat_log
                        return-void
                    """.trimIndent(),
                ),
            )

            val setNickname = it.instructionMatches.last().index
            val nicknameView = it.method.getInstruction<FiveRegisterInstruction>(setNickname).registerC
            it.method.addInstruction(
                setNickname + 1,
                "invoke-virtual {p0, v$nicknameView}, $viewHolder->$BIND_NICKNAME_METHOD(Landroid/widget/TextView;)V",
            )
        }
    }
}
