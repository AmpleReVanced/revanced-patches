package app.revanced.patches.kakaotalk.interaction.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.cloneParameters
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.cloneMutable
import app.morphe.util.setExtensionIsPatchIncluded
import app.morphe.util.transformMethods
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatContextMenuActionLabelFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogContextMenuActionsFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.threadMessageActionFilterFingerprint
import app.revanced.patches.kakaotalk.misc.extension.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.extension.sharedExtensionPatch
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.addKakaoTalkResources
import app.revanced.util.parameterTypeNames
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

private const val DETAILS_PACKAGE = "Lapp/revanced/extension/kakaotalk/chatlog/details"
private const val EXTENSION_CLASS = "$DETAILS_PACKAGE/MessageDetailsExtension;"
private const val ACTION_CLASS = "$DETAILS_PACKAGE/MessageDetailsAction;"
private const val SOURCE_INTERFACE = "$DETAILS_PACKAGE/MessageDetailsSource;"

private val registerMessageDetailsActivityPatch = patch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activity = document.createElement("activity")
            activity.setAttribute("android:name", "app.revanced.extension.kakaotalk.chatlog.details.MessageDetailsActivity")
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:excludeFromRecents", "true")
            activity.setAttribute("android:label", "@string/morphe_kakaotalk_message_details_title")
            activity.setAttribute("android:theme", "@style/Theme.Default.NoActionBar")
            application.appendChild(activity)
        }
    }
}

@Suppress("unused")
val showMessageDetailsPatch = patch(
    name = "Show message details",
    description = "Adds a long-press action to inspect the complete ChatLog object, including inherited and message-specific fields, as JSON.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(
        addExtensionPatch,
        addResourcesPatch,
        addSettingsTabPatch,
        sharedExtensionPatch,
        registerMessageDetailsActivityPatch,
    )

    execute {
        addKakaoTalkResources()
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_show_message_details",
                titleKey = "morphe_settings_patch_show_message_details",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val chatLogClass = ChatLogFingerprint.classDef
        val chatLogType = chatLogClass.type
        chatLogClass.interfaces.add(SOURCE_INTERFACE)
        annotateMessageDetailsFields(chatLogClass)

        val actionBase = ChatContextMenuActionLabelFingerprint.classDef
        val actionType = actionBase.type
        val constructor = actionBase.methods.singleOrNull {
            it.name == "<init>" && AccessFlags.PUBLIC.isSet(it.accessFlags) &&
                it.parameterTypeNames.let { parameters ->
                    parameters.size == 4 && parameters.first() == chatLogType &&
                        parameters.takeLast(2) == listOf("Ljava/lang/Integer;", "Lkotlin/jvm/internal/DefaultConstructorMarker;")
                }
        } ?: throw PatchException("Could not resolve the message action constructor.")
        val roomType = constructor.parameterTypeNames[1]
        val performAction = actionBase.methods.singleOrNull {
            AccessFlags.ABSTRACT.isSet(it.accessFlags) &&
                it.parameterTypeNames == listOf("Landroidx/fragment/app/FragmentActivity;") && it.returnType == "V"
        } ?: throw PatchException("Could not resolve the message action callback.")
        val supportedTypes = actionBase.methods.singleOrNull {
            AccessFlags.ABSTRACT.isSet(it.accessFlags) &&
                it.parameterTypes.isEmpty() && it.returnType == "Ljava/util/Set;"
        } ?: throw PatchException("Could not resolve the message action types.")

        mutableClassDefBy(ACTION_CLASS).apply {
            setSuperClass(actionType)
            transformMethods {
                when (name) {
                    "<init>" -> cloneMutable(additionalRegisters = 5).apply {
                        removeInstructions(0, instructions.count())
                        addInstructionsWithLabels(
                            0,
                            """
                                move-object v0, p0
                                move-object v1, p1
                                check-cast v1, $chatLogType
                                move-object v2, p2
                                check-cast v2, $roomType
                                const/4 v3, 0x0
                                const/4 v4, 0x0
                                invoke-direct/range {v0 .. v4}, ${constructor.smaliReference}
                                check-cast p1, $SOURCE_INTERFACE
                                iput-object p1, p0, $ACTION_CLASS->message:$SOURCE_INTERFACE
                                return-void
                            """,
                        )
                    }
                    "getLabel" -> cloneMutable(name = ChatContextMenuActionLabelFingerprint.method.name)
                    "getSupportedTypes" -> cloneMutable(name = supportedTypes.name)
                    "perform" -> cloneMutable(name = performAction.name, parameters = performAction.parameters)
                    else -> this
                }
            }
        }

        addMessageDetailsMenus(chatLogType)

        ChatLogContextMenuActionsFingerprint.apply {
            val result = instructionMatches[1]
            val listRegister = result.getInstruction<OneRegisterInstruction>().registerA
            val insertIndex = result.index + 1
            val registers = method.getFreeRegisterProvider(insertIndex, 2, listRegister)
            val roomRegister = registers.getFreeRegister4Bit()
            val messageRegister = registers.getFreeRegister4Bit()
            val message = classDef.instanceFields.single { it.type == chatLogType }
            val room = classDef.instanceFields.single { it.type == roomType }

            method.addInstructions(
                insertIndex,
                """
                    move-object/from16 v$roomRegister, p0
                    iget-object v$messageRegister, v$roomRegister, ${message.smaliReference}
                    iget-object v$roomRegister, v$roomRegister, ${room.smaliReference}
                    invoke-static {v$listRegister, v$messageRegister, v$roomRegister}, $EXTENSION_CLASS->prependAction(Ljava/util/List;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;
                    move-result-object v$listRegister
                """,
            )
        }

        threadMessageActionFilterFingerprint(actionType).method.cloneParameters().addInstructionsWithLabels(
            0,
            """
                instance-of v0, p1, $ACTION_CLASS
                if-eqz v0, :original_filter
                const/4 v0, 0x1
                return v0
                :original_filter
                nop
            """,
        )
    }
}