package app.revanced.patches.kakaotalk.interaction.ghost

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.addInstructionsAtControlFlowLabel
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.misc.settings.preference.MORPHE_PREFERENCE_SCREEN_LAYOUT
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/GhostModePatch;"

private const val LOCO_IGNORABLE_EXCEPTION =
    "Lcom/kakao/talk/core/loco/exception/LocoIgnorableException;"

private val LOCAL_READ_SOURCES = setOf(
    "ChatLogController.kt",
    "ChatThreadChatLogController.kt",
    "ChatRoomController.kt",
    "ChatSendingLogRequest.kt",
)

private val READ_ELSEWHERE_SOURCES = setOf("LocoDecUnreadPush.kt", "ClearNotiPushTask.kt")

private fun MethodReference.forEachCallIn(sourceFiles: Set<String>, block: MutableMethod.(Int) -> Unit) =
    Fingerprint(
        filters = listOf(methodCall(this)),
        custom = { _, classDef -> classDef.sourceFile in sourceFiles },
    ).matchAllMethodIndicesForEach(block = block)

@Suppress("unused")
val ghostMode = patch(
    name = "Ghost Mode",
    description = "Adds options to hide your typing status and to keep chats from being marked as read.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            PreferenceScreenPreference(
                key = "morphe_pref_ghost_mode_screen",
                titleKey = "morphe_settings_section_ghost_mode",
                summaryKey = "morphe_settings_section_ghost_mode_summary",
                layout = MORPHE_PREFERENCE_SCREEN_LAYOUT,
                preferences = setOf(
                    SwitchPreference(
                        key = "morphe_pref_ghost_mode",
                        titleKey = "morphe_settings_patch_ghost_mode",
                        summary = true,
                    ),
                    SwitchPreference(
                        key = "morphe_pref_ghost_read",
                        titleKey = "morphe_settings_patch_ghost_read",
                        summary = true,
                    ),
                    SwitchPreference(
                        key = "morphe_pref_ghost_read_keep_unread",
                        titleKey = "morphe_settings_patch_ghost_read_keep_unread",
                        summary = true,
                    ),
                    SwitchPreference(
                        key = "morphe_pref_ghost_read_on_send",
                        titleKey = "morphe_settings_patch_ghost_read_on_send",
                        summary = true,
                    ),
                ),
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val locoMethodClass = LocoMethodClassFingerprint.classDef
        val actionJobClass = actionJobMethodFingerprint(locoMethodClass).classDef
        val sendActionMethod = sendCurrentActionFingerprint(actionJobClass).method
        val protocolSuccessClass = ProtocolSuccessFingerprint.classDef
        val registers = sendActionMethod.getFreeRegisterProvider(0, 2)
        val valueRegister = registers.getFreeRegister4Bit()
        val resultRegister = registers.getFreeRegister4Bit()

        sendActionMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->enableGhostMode()Z
                move-result v$valueRegister
                if-eqz v$valueRegister, :morphe_original
                const/4 v$valueRegister, 0x0
                new-instance v$resultRegister, ${protocolSuccessClass.type}
                invoke-direct {v$resultRegister, v$valueRegister}, ${protocolSuccessClass.type}-><init>(Ljava/lang/Object;)V
                return-object v$resultRegister
                :morphe_original
                nop
            """
        )

        ChatOnRoomRequestFingerprint.method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, Lapp/revanced/extension/kakaotalk/settings/Settings;->enableGhostRead()Z
                    move-result v$register
                    if-eqz v$register, :ghost_read_off
                    new-instance v$register, $LOCO_IGNORABLE_EXCEPTION
                    invoke-direct {v$register}, $LOCO_IGNORABLE_EXCEPTION-><init>()V
                    throw v$register
                """,
                ExternalLabel("ghost_read_off", getInstruction(0)),
            )
        }

        val readPointer = ReadPointerFingerprint.method
        val readLogId = ReadPointerFingerprint.readLogId
        MessageUnreadCountFingerprint.let {
            val (type, yours, userId, logId, unreadCount) = it.instructionMatches.map { match ->
                match.instruction.getReference<Reference>()!!
            }
            it.method.addInstructionsWithLabels(
                0,
                """
                    iget-object v0, p0, $type
                    sget-object v1, $yours
                    if-ne v0, v1, :ghost_unread_off
                    iget-wide v0, p0, $logId
                    iget-wide v2, p1, $readLogId
                    invoke-static/range {v0 .. v3}, $EXTENSION_CLASS->isUnreadByMe(JJ)Z
                    move-result v2
                    if-eqz v2, :ghost_unread_off
                    iget-wide v2, p0, $userId
                    invoke-virtual {p1, v2, v3, v0, v1}, $unreadCount
                    move-result v0
                    add-int/lit8 v0, v0, 0x1
                    const/16 v1, 0x63
                    invoke-static {v0, v1}, Ljava/lang/Math;->min(II)I
                    move-result v0
                    return v0
                """,
                ExternalLabel("ghost_unread_off", it.method.getInstruction(0)),
            )
        }

        val localReads = ReadPointerFingerprint.classDef.methods.filter { method ->
            method.parameterTypes.firstOrNull() == "J" &&
                method.instructions.any { it.getReference<MethodReference>() == readPointer }
        }
        localReads.forEach { localRead ->
            localRead.forEachCallIn(LOCAL_READ_SOURCES) { index ->
                val logId = getInstruction<FiveRegisterInstruction>(index).registerD
                addInstructionsAtControlFlowLabel(
                    index,
                    """
                        invoke-static/range {v$logId .. v${logId + 1}}, $EXTENSION_CLASS->localReadLogId(J)J
                        move-result-wide v$logId
                    """,
                )
            }
        }

        val markRead = localReads.single { it.parameterTypes.size == 1 }
        SentChatLogFingerprint.let {
            val (visibleCheck, readPointerCall, result) = it.instructionMatches
            val call = readPointerCall.getInstruction<FiveRegisterInstruction>()
            it.method.addInstruction(
                result.index + 1,
                "invoke-virtual {v${call.registerC}, v${call.registerE}, v${call.registerF}}, $markRead",
            )
            it.method.addInstructions(
                readPointerCall.index,
                """
                    invoke-static/range {v${call.registerE} .. v${call.registerF}}, $EXTENSION_CLASS->sentLogId(J)J
                    move-result-wide v${call.registerE}
                """,
            )

            visibleCheck.methodReference.forEachCallIn(READ_ELSEWHERE_SOURCES) { index ->
                val visible = getInstruction<OneRegisterInstruction>(index + 1).registerA
                addInstructions(
                    index + 2,
                    """
                        invoke-static {v$visible}, $EXTENSION_CLASS->isChatRoomVisible(Z)Z
                        move-result v$visible
                    """,
                )
            }
        }

        ReceivedChatLogFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range {p2 .. p2}, $EXTENSION_CLASS->isChatRoomVisible(Z)Z
                move-result p2
            """,
        )

        NoSeenBodyFingerprint.matchAll().forEach { match ->
            val put = match.instructionMatches.last()
            val value = put.getInstruction<FiveRegisterInstruction>().registerE
            match.method.addInstructions(
                put.index,
                """
                    invoke-static {v$value}, $EXTENSION_CLASS->noSeen(Ljava/lang/Boolean;)Ljava/lang/Boolean;
                    move-result-object v$value
                """,
            )
        }
    }
}
