package app.revanced.patches.kakaotalk.interaction.hiddenauthor

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.addInstructionsAtControlFlowLabel
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.interaction.chatlog.bridgeMethod
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatMemberSetConstructorFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.FriendDisplayNameFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.FriendPrimaryConstructorFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.OpenLinkIdGetterFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.chatRoomMemberSetGetterFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.normalMemberProfileFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.openMemberProfileFingerprint
import app.revanced.patches.kakaotalk.misc.extension.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.extension.sharedExtensionPatch
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.addKakaoTalkResources
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS = "Lapp/revanced/extension/kakaotalk/patches/HiddenMessageAuthorPatch;"
private const val MEMBER_METHOD = "morphe_hiddenMessageAuthor"
private const val MEMBER_NAME_METHOD = "morphe_hiddenMessageAuthorName"
private const val MEMBER_PROFILE_METHOD = "morphe_openHiddenMessageAuthorProfile"
private const val RECORD_AUTHOR_METHOD = "morphe_recordHiddenMessageAuthor"

@Suppress("unused")
val showHiddenMessageAuthorPatch = patch(
    name = "Show who hid messages",
    description = "Shows the nickname of the host or staff member who hid messages in open chats.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch, addSettingsTabPatch, sharedExtensionPatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_show_hidden_message_author",
                titleKey = "morphe_settings_patch_show_hidden_message_author",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)
        addKakaoTalkResources()

        val chatRoomType = RewritesFeedTaskFingerprint.method.parameterTypes[1].toString()
        val memberSetType = ChatMemberSetConstructorFingerprint.classDef.type
        val friendType = FriendPrimaryConstructorFingerprint.classDef.type
        val memberSetGetter = chatRoomMemberSetGetterFingerprint(chatRoomType, memberSetType).method
        val memberSetOwner = ChatMemberSetConstructorFingerprint.method.instructions
            .first { it.opcode == Opcode.IPUT_WIDE }.getReference<FieldReference>()!!
        val navigator = MemberProfileNavigatorFingerprint.classDef
        val member = bridgeMethod(
            definingClass = chatRoomType,
            name = MEMBER_METHOD,
            parameterTypes = listOf("J"),
            returnType = friendType,
            registerCount = 12,
            instructions = """
                invoke-virtual {p0}, ${memberSetGetter.smaliReference}
                move-result-object v0
                if-eqz v0, :none
                move-wide v1, p1
                const/4 v5, 0x0
                invoke-virtual {v0}, ${OpenLinkIdGetterFingerprint.method.smaliReference}
                move-result-wide v3
                const-wide/16 v6, 0x0
                cmp-long v8, v3, v6
                if-gtz v8, :open
                invoke-virtual {v0, v1, v2, v5}, ${normalMemberProfileFingerprint(friendType).method.smaliReference}
                move-result-object v0
                return-object v0
                :open
                invoke-virtual/range {v0 .. v5}, ${openMemberProfileFingerprint(friendType).method.smaliReference}
                move-result-object v0
                return-object v0
                :none
                const/4 v0, 0x0
                return-object v0
            """,
        )
        mutableClassDefBy(chatRoomType).methods += listOf(
            member,
            bridgeMethod(
                definingClass = chatRoomType,
                name = MEMBER_NAME_METHOD,
                parameterTypes = listOf("J"),
                returnType = "Ljava/lang/String;",
                registerCount = 4,
                instructions = """
                    invoke-virtual {p0, p1, p2}, ${member.smaliReference}
                    move-result-object v0
                    if-eqz v0, :none
                    invoke-virtual {v0}, ${FriendDisplayNameFingerprint.method.smaliReference}
                    move-result-object v0
                    return-object v0
                    :none
                    const/4 v0, 0x0
                    return-object v0
                """,
            ),
            bridgeMethod(
                definingClass = chatRoomType,
                name = MEMBER_PROFILE_METHOD,
                parameterTypes = listOf("Landroid/content/Context;", "J"),
                returnType = "V",
                registerCount = 8,
                instructions = """
                    invoke-virtual {p0, p2, p3}, ${member.smaliReference}
                    move-result-object v0
                    if-eqz v0, :done
                    invoke-virtual {p0}, ${memberSetGetter.smaliReference}
                    move-result-object v1
                    iget-wide v2, v1, ${memberSetOwner.smaliReference}
                    new-instance v1, ${navigator.type}
                    invoke-direct {v1, p1, v2, v3, p0}, ${navigator.methods.single { it.name == "<init>" }.smaliReference}
                    invoke-virtual {v1, v0}, ${MemberProfileNavigatorFingerprint.method.smaliReference}
                    :done
                    return-void
                """,
            ),
        )

        RewritesFeedTaskFingerprint.let { task ->
            val (author, _, _, logId, message) = LocoChatLogFeedFingerprint.instructionMatches.map { it.methodReference }
            task.classDef.methods.add(
                bridgeMethod(
                    definingClass = task.classDef.type,
                    name = RECORD_AUTHOR_METHOD,
                    parameterTypes = listOf("Ljava/lang/Object;"),
                    returnType = "V",
                    registerCount = 8,
                    instructions = """
                        iget-object v5, p0, ${task.feed.smaliReference}
                        if-eqz v5, :done
                        move-object v0, p1
                        invoke-virtual {v5}, $author
                        move-result-wide v1
                        invoke-virtual {v5}, $logId
                        move-result-wide v3
                        invoke-virtual {v5}, $message
                        move-result-object v5
                        invoke-static/range {v0 .. v5}, $EXTENSION_CLASS->onRewritesFeed(Ljava/lang/Object;JJLjava/lang/String;)V
                        :done
                        return-void
                    """,
                ),
            )
            task.method.addInstruction(
                0,
                "invoke-virtual {p0, p3}, ${task.classDef.type}->$RECORD_AUTHOR_METHOD(Ljava/lang/Object;)V",
            )
        }

        FeedMessageFingerprint.method.apply {
            instructions.indices.filter { instructions[it].opcode == Opcode.RETURN_OBJECT }.asReversed().forEach { index ->
                val message = getInstruction<OneRegisterInstruction>(index).registerA
                addInstructionsAtControlFlowLabel(
                    index,
                    """
                        invoke-static {v$message, p1, p2}, $EXTENSION_CLASS->feedMessage(Ljava/lang/CharSequence;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/CharSequence;
                        move-result-object v$message
                    """,
                )
            }
        }
    }
}
