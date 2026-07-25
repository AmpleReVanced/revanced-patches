package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.getReference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogItemViewHolderFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogViewHolderBindProfileFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogViewHolderSetupChatInfoViewFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatMemberSetConstructorFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatRoomListManagerGetInstanceFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.FriendDisplayNameFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.FriendPrimaryConstructorFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.FriendProfileImageUrlFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.GetChatRoomByChannelIdFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.MemberWatermarksGetterFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.OpenLinkIdGetterFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.activeMembersCountGetterFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.chatRoomMemberSetGetterFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.currentUserPredicateFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.normalMemberProfileFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.openMemberProfileFingerprint
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.sharedExtensionPatch
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.addKakaoTalkResources
import app.revanced.util.matches
import app.revanced.util.parameterRegister
import app.revanced.util.parameterTypeNames
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import org.w3c.dom.Element

private const val CHAT_LOG_ID_METHOD = "revanced_read_receipt_log_id"
private const val CHAT_ID_METHOD = "revanced_read_receipt_chat_id"
private const val SENDER_ID_METHOD = "revanced_read_receipt_sender_id"
private const val MEMBER_SET_BY_CHAT_ID_METHOD = "revanced_read_receipt_member_set_by_chat_id"
private const val WATERMARKS_METHOD = "revanced_read_receipt_watermarks"
private const val ACTIVE_COUNT_METHOD = "revanced_read_receipt_active_count"
private const val CURRENT_USER_ID_METHOD = "revanced_read_receipt_current_user_id"
private const val IS_OPEN_CHAT_METHOD = "revanced_read_receipt_is_open_chat"
private const val PROFILE_METHOD = "revanced_read_receipt_profile"
private const val PROFILE_NAME_METHOD = "revanced_read_receipt_name"
private const val PROFILE_IMAGE_URL_METHOD = "revanced_read_receipt_image_url"
private const val PROFILE_IMAGE_TYPE_METHOD = "revanced_read_receipt_image_type"
private const val PROFILE_COMPLETE_METHOD = "revanced_read_receipt_profile_complete"
private const val BIND_CHAT_INFO_VIEW_METHOD = "revanced_read_receipt_bind_chat_info_view"
private const val PROFILE_VIEW_CLASS = "Lcom/kakao/talk/widget/ProfileView;"
private const val CHAT_INFO_VIEW_CLASS = "Lcom/kakao/talk/widget/chatlog/ChatInfoView;"
private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/chatlog/readreceipt/MessageReadReceiptsExtension;"
private const val PATCH_MARKER_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/ShowMessageReadReceiptsPatch;"

private val registerMessageReadReceiptsActivityPatch = resourcePatch {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activityName = "app.revanced.extension.kakaotalk.chatlog.readreceipt.MessageReadReceiptsActivity"
            val activities = application.getElementsByTagName("activity")

            for (index in 0 until activities.length) {
                val activity = activities.item(index) as? Element ?: continue
                if (activity.getAttribute("android:name") == activityName) {
                    activity.setAttribute("android:excludeFromRecents", "true")
                    activity.setAttribute("android:exported", "false")
                    activity.setAttribute("android:launchMode", "singleTop")
                    activity.setAttribute("android:label", "@string/morphe_kakaotalk_chatlog_read_receipts_title")
                    activity.setAttribute("android:theme", "@style/Theme.Default.NoActionBar")
                    return@use
                }
            }

            val activity = document.createElement("activity")
            activity.setAttribute("android:name", activityName)
            activity.setAttribute("android:excludeFromRecents", "true")
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:launchMode", "singleTop")
            activity.setAttribute("android:label", "@string/morphe_kakaotalk_chatlog_read_receipts_title")
            activity.setAttribute("android:theme", "@style/Theme.Default.NoActionBar")
            application.appendChild(activity)
        }
    }
}

@Suppress("unused")
val showMessageReadReceiptsPatch = bytecodePatch(
    name = "Show message read receipts",
    description = "Shows the members who have or have not read each chat message.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(
        addExtensionPatch,
        addResourcesPatch,
        addSettingsTabPatch,
        sharedExtensionPatch,
        registerMessageReadReceiptsActivityPatch,
    )

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_show_message_read_receipts",
                titleKey = "morphe_settings_patch_show_message_read_receipts",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(PATCH_MARKER_CLASS)
        addKakaoTalkResources()

        val memberSetConstructor = ChatMemberSetConstructorFingerprint.method
        val memberSetClass = ChatMemberSetConstructorFingerprint.classDef
        val memberSetType = memberSetClass.type
        val activeMembersCountField = memberSetConstructor.requireParameterBackedField(
            parameterIndex = 4,
            opcode = Opcode.IPUT,
            expectedType = "I",
        )

        val friendClass = FriendPrimaryConstructorFingerprint.classDef
        val friendType = friendClass.type
        val displayNameMethod = FriendDisplayNameFingerprint.method
        val profileImageUrlMethod = FriendProfileImageUrlFingerprint.method

        val watermarksGetter = MemberWatermarksGetterFingerprint.method
        val watermarkManagerGetterReference = watermarksGetter.instructions
            .mapNotNull { it.getReference<MethodReference>() }
            .singleOrNull { reference ->
                reference.parameterTypeNames.isEmpty() &&
                    reference.returnType == "Ljava/util/Map;"
            }
            ?: throw PatchException("Could not resolve watermark manager getter call.")
        val watermarkManagerField = watermarksGetter.instructions
            .asSequence()
            .filter { it.opcode == Opcode.IGET_OBJECT }
            .mapNotNull { it.getReference<FieldReference>() }
            .singleOrNull { field ->
                field.definingClass == memberSetType &&
                    field.type == watermarkManagerGetterReference.definingClass
            }
            ?: throw PatchException("Could not resolve ChatMemberSet watermark manager field.")
        val watermarkManagerClass = mutableClassDefBy(watermarkManagerField.type)
        val watermarkManagerGetter = watermarkManagerClass.methods
            .singleOrNull(watermarkManagerGetterReference::matches)
            ?: throw PatchException("Could not resolve watermark manager getter.")
        // The getter returns an unmodifiable view; the extension needs the backing map to lock on.
        val watermarkMapField = watermarkManagerGetter.implementation?.instructions
            ?.asSequence()
            ?.filter { it.opcode == Opcode.IGET_OBJECT }
            ?.mapNotNull { it.getReference<FieldReference>() }
            ?.singleOrNull { field ->
                field.definingClass == watermarkManagerField.type &&
                    field.type == "Ljava/util/Map;"
            }
            ?: throw PatchException("Could not resolve synchronized watermark map field.")
        val activeCountGetter = activeMembersCountGetterFingerprint(activeMembersCountField).method
        val openLinkIdGetter = OpenLinkIdGetterFingerprint.method
        val normalProfileGetter = normalMemberProfileFingerprint(friendType).method
        val openProfileGetter = openMemberProfileFingerprint(friendType).method
        if (normalProfileGetter.parameterTypeNames[1] != openProfileGetter.parameterTypeNames[2]) {
            throw PatchException("ChatMemberSet profile resolver parameter types do not match.")
        }
        val currentUserReferences = normalProfileGetter.implementation?.instructions
            ?.mapNotNull { it.getReference<MethodReference>() }
            ?.zipWithNext()
            ?.singleOrNull { (provider, predicate) ->
                provider.returnType.startsWith("L") &&
                    predicate.definingClass == provider.returnType &&
                    predicate.parameterTypeNames == listOf("J") &&
                    predicate.returnType == "Z"
            }
            ?: throw PatchException("Could not resolve current user predicate calls.")
        val meType = currentUserReferences.first.returnType
        val currentUserPredicate = currentUserPredicateFingerprint(meType)
        val currentUserPredicateMethod = currentUserPredicate.method
        // The reference is to the interface, so only the signature can be compared.
        if (!currentUserReferences.second.matches(currentUserPredicateMethod)) {
            throw PatchException("Current user predicate fingerprint resolved an incompatible method.")
        }
        val currentUserIdReference = currentUserPredicateMethod.implementation?.instructions
            ?.mapNotNull { it.getReference<MethodReference>() }
            ?.singleOrNull { reference ->
                reference.definingClass == currentUserPredicateMethod.definingClass &&
                    reference.parameterTypeNames.isEmpty() &&
                    reference.returnType == "J"
            }
            ?: throw PatchException("Could not resolve current user ID getter call.")
        val meImplementationClass = currentUserPredicate.classDef
        val meInstanceField = meImplementationClass.fields.singleOrNull { field ->
            AccessFlags.STATIC.isSet(field.accessFlags) &&
                field.type == meImplementationClass.type
        } ?: throw PatchException("Could not resolve current user provider instance.")
        val profileStateReferences = normalProfileGetter.implementation?.instructions
            ?.mapNotNull { it.getReference<MethodReference>() }
            ?.zipWithNext()
            ?.singleOrNull { (stateGetter, stateComplete) ->
                stateGetter.definingClass == friendType &&
                    stateGetter.parameterTypeNames.isEmpty() &&
                    stateGetter.returnType.startsWith("L") &&
                    stateComplete.definingClass == stateGetter.returnType &&
                    stateComplete.parameterTypeNames.isEmpty() &&
                    stateComplete.returnType == "Z"
            }
            ?: throw PatchException("Could not resolve Friend profile completion calls.")
        val profileStateGetter = profileStateReferences.first
        val profileStateComplete = profileStateReferences.second
        val profileStateType = profileStateGetter.returnType

        // The image type follows the member's own user status, not the kind of chat room.
        val openProfileStateField = ChatLogViewHolderBindProfileFingerprint.method
            .profileImageTypeConstant(profileStateType)

        friendClass.methods.addAll(
            friendBridgeMethods(
                friendType = friendType,
                displayNameMethod = displayNameMethod,
                profileImageUrlMethod = profileImageUrlMethod,
                profileStateGetter = profileStateGetter,
                profileStateComplete = profileStateComplete,
                openProfileStateField = openProfileStateField,
            ),
        )

        memberSetClass.methods.addAll(
            memberSetBridgeMethods(
                memberSetType = memberSetType,
                watermarkManagerField = watermarkManagerField,
                watermarkMapField = watermarkMapField,
                activeCountGetter = activeCountGetter,
                meInstanceField = meInstanceField,
                currentUserIdReference = currentUserIdReference,
                openLinkIdGetter = openLinkIdGetter,
                normalProfileGetter = normalProfileGetter,
                openProfileGetter = openProfileGetter,
            ),
        )

        val chatLogClass = ChatLogFingerprint.classDef
        val chatLogType = chatLogClass.type
        val chatLogIdentityMethod = ChatLogFingerprint.method
        val chatLogIdField = chatLogIdentityMethod.requirePrintedWideField("] ChatLog [id=")
        val chatLogChatIdField = chatLogIdentityMethod.requirePrintedWideField(", chatRoomId=")
        val chatLogSenderIdField = chatLogIdentityMethod.requirePrintedWideField(", userId=")

        val getManagerInstance = ChatRoomListManagerGetInstanceFingerprint.method
        val getChatRoom = GetChatRoomByChannelIdFingerprint.method
        if (getManagerInstance.returnType != getChatRoom.definingClass) {
            throw PatchException("ChatRoomListManager fingerprints resolved incompatible methods.")
        }
        val managerClass = GetChatRoomByChannelIdFingerprint.classDef
        val managerCompanionField = managerClass.fields.singleOrNull { field ->
            AccessFlags.STATIC.isSet(field.accessFlags) &&
                field.type == getManagerInstance.definingClass
        } ?: throw PatchException("Could not resolve ChatRoomListManager companion field.")
        val chatRoomType = getChatRoom.returnType
        val memberSetGetter = chatRoomMemberSetGetterFingerprint(chatRoomType, memberSetType).method

        chatLogClass.methods.addAll(
            chatLogBridgeMethods(
                chatLogType = chatLogType,
                chatLogIdField = chatLogIdField,
                chatLogChatIdField = chatLogChatIdField,
                chatLogSenderIdField = chatLogSenderIdField,
                managerCompanionField = managerCompanionField,
                getManagerInstance = getManagerInstance,
                getChatRoom = getChatRoom,
                memberSetGetter = memberSetGetter,
            ),
        )

        val setupChatInfoViewMethod = ChatLogViewHolderSetupChatInfoViewFingerprint.method
        val getChatLogItemMethod = ChatLogItemViewHolderFingerprint.method
        val setModifyIndex = setupChatInfoViewMethod.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                instruction.getReference<MethodReference>()?.let { reference ->
                    reference.definingClass == CHAT_INFO_VIEW_CLASS &&
                        reference.name == "setModify" &&
                        reference.parameterTypeNames == listOf("Z") &&
                        reference.returnType == "V"
                } == true
        }.takeIf { it >= 0 }
            ?: throw PatchException("Could not find ChatInfoView modify binding call.")

        val chatInfoViewRegister =
            (setupChatInfoViewMethod.getInstruction(setModifyIndex) as FiveRegisterInstruction)
                .registerC

        // Other chat log patches inject here too, leaving no free register to unpack the chat log
        // with, so that work lives in a method of its own.
        val viewHolderClass = ChatLogViewHolderSetupChatInfoViewFingerprint.classDef
        viewHolderClass.methods.add(
            bridgeMethod(
                definingClass = viewHolderClass.type,
                name = BIND_CHAT_INFO_VIEW_METHOD,
                parameterTypes = listOf("Landroid/view/View;"),
                returnType = "V",
                registerCount = 4,
                instructions = """
                    invoke-virtual {p0}, ${getChatLogItemMethod.smaliReference}
                    move-result-object v0
                    instance-of v1, v0, $chatLogType
                    if-eqz v1, :revanced_read_receipt_not_chat_log
                    check-cast v0, $chatLogType
                    goto :revanced_read_receipt_bind
                    :revanced_read_receipt_not_chat_log
                    const/4 v0, 0x0
                    :revanced_read_receipt_bind
                    invoke-static {p1, v0}, $EXTENSION_CLASS->bind(Landroid/view/View;Ljava/lang/Object;)V
                    return-void
                """.trimIndent(),
            ),
        )

        setupChatInfoViewMethod.addInstructionsWithLabels(
            setModifyIndex + 1,
            "invoke-virtual {p0, v$chatInfoViewRegister}, " +
                "${viewHolderClass.type}->$BIND_CHAT_INFO_VIEW_METHOD(Landroid/view/View;)V",
        )
    }
}

private fun friendBridgeMethods(
    friendType: String,
    displayNameMethod: Method,
    profileImageUrlMethod: Method,
    profileStateGetter: MethodReference,
    profileStateComplete: MethodReference,
    openProfileStateField: FieldReference,
) = listOf(
    bridgeMethod(
        definingClass = friendType,
        name = PROFILE_NAME_METHOD,
        returnType = "Ljava/lang/String;",
        registerCount = 2,
        instructions = """
            invoke-virtual {p0}, ${displayNameMethod.smaliReference}
            move-result-object v0
            return-object v0
        """.trimIndent(),
    ),
    bridgeMethod(
        definingClass = friendType,
        name = PROFILE_IMAGE_URL_METHOD,
        returnType = "Ljava/lang/String;",
        registerCount = 2,
        instructions = """
            invoke-virtual {p0}, ${profileImageUrlMethod.smaliReference}
            move-result-object v0
            return-object v0
        """.trimIndent(),
    ),
    bridgeMethod(
        definingClass = friendType,
        name = PROFILE_IMAGE_TYPE_METHOD,
        returnType = "I",
        registerCount = 3,
        instructions = """
            invoke-virtual {p0}, ${profileStateGetter.smaliReference}
            move-result-object v0
            sget-object v1, ${openProfileStateField.smaliReference}
            if-ne v0, v1, :revanced_default_profile_image
            const/4 v0, -0x1
            return v0
            :revanced_default_profile_image
            const/4 v0, 0x0
            return v0
        """.trimIndent(),
    ),
    bridgeMethod(
        definingClass = friendType,
        name = PROFILE_COMPLETE_METHOD,
        returnType = "Z",
        registerCount = 2,
        instructions = """
            invoke-virtual {p0}, ${profileStateGetter.smaliReference}
            move-result-object v0
            invoke-virtual {v0}, ${profileStateComplete.smaliReference}
            move-result v0
            return v0
        """.trimIndent(),
    ),
)

private fun memberSetBridgeMethods(
    memberSetType: String,
    watermarkManagerField: FieldReference,
    watermarkMapField: FieldReference,
    activeCountGetter: Method,
    meInstanceField: Field,
    currentUserIdReference: MethodReference,
    openLinkIdGetter: Method,
    normalProfileGetter: Method,
    openProfileGetter: Method,
) = listOf(
    bridgeMethod(
        definingClass = memberSetType,
        name = WATERMARKS_METHOD,
        returnType = "Ljava/lang/Object;",
        registerCount = 2,
        instructions = """
            iget-object v0, p0, ${watermarkManagerField.smaliReference}
            iget-object v0, v0, ${watermarkMapField.smaliReference}
            invoke-static {v0}, $EXTENSION_CLASS->snapshotWatermarks(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object v0
            return-object v0
        """.trimIndent(),
    ),
    bridgeMethod(
        definingClass = memberSetType,
        name = ACTIVE_COUNT_METHOD,
        returnType = "I",
        registerCount = 2,
        instructions = """
            invoke-virtual {p0}, ${activeCountGetter.smaliReference}
            move-result v0
            return v0
        """.trimIndent(),
    ),
    bridgeMethod(
        definingClass = memberSetType,
        name = CURRENT_USER_ID_METHOD,
        returnType = "J",
        registerCount = 3,
        instructions = """
            sget-object v0, ${meInstanceField.smaliReference}
            invoke-virtual {v0}, ${currentUserIdReference.smaliReference}
            move-result-wide v0
            return-wide v0
        """.trimIndent(),
    ),
    bridgeMethod(
        definingClass = memberSetType,
        name = IS_OPEN_CHAT_METHOD,
        returnType = "Z",
        registerCount = 5,
        instructions = """
            invoke-virtual {p0}, ${openLinkIdGetter.smaliReference}
            move-result-wide v0
            const-wide/16 v2, 0x0
            cmp-long v0, v0, v2
            if-lez v0, :revanced_normal_chat
            const/4 v0, 0x1
            return v0
            :revanced_normal_chat
            const/4 v0, 0x0
            return v0
        """.trimIndent(),
    ),
    bridgeMethod(
        definingClass = memberSetType,
        name = PROFILE_METHOD,
        parameterTypes = listOf("J"),
        returnType = "Ljava/lang/Object;",
        registerCount = 9,
        instructions = """
            invoke-virtual {p0}, $memberSetType->$IS_OPEN_CHAT_METHOD()Z
            move-result v0
            if-eqz v0, :revanced_normal_profile
            move-object v0, p0
            move-wide v1, p1
            invoke-virtual {p0}, ${openLinkIdGetter.smaliReference}
            move-result-wide v3
            const/4 v5, 0x0
            invoke-virtual/range {v0 .. v5}, ${openProfileGetter.smaliReference}
            move-result-object v0
            return-object v0
            :revanced_normal_profile
            move-object v0, p0
            move-wide v1, p1
            const/4 v5, 0x0
            invoke-virtual {v0, v1, v2, v5}, ${normalProfileGetter.smaliReference}
            move-result-object v0
            return-object v0
        """.trimIndent(),
    ),
)

private fun chatLogBridgeMethods(
    chatLogType: String,
    chatLogIdField: FieldReference,
    chatLogChatIdField: FieldReference,
    chatLogSenderIdField: FieldReference,
    managerCompanionField: Field,
    getManagerInstance: Method,
    getChatRoom: Method,
    memberSetGetter: Method,
) = listOf(
    wideFieldGetter(chatLogType, CHAT_LOG_ID_METHOD, chatLogIdField),
    wideFieldGetter(chatLogType, CHAT_ID_METHOD, chatLogChatIdField),
    wideFieldGetter(chatLogType, SENDER_ID_METHOD, chatLogSenderIdField),
    bridgeMethod(
        definingClass = chatLogType,
        name = MEMBER_SET_BY_CHAT_ID_METHOD,
        parameterTypes = listOf("J"),
        returnType = "Ljava/lang/Object;",
        accessFlags = AccessFlags.PUBLIC.value or AccessFlags.STATIC.value or AccessFlags.FINAL.value,
        registerCount = 3,
        instructions = """
            sget-object v0, ${managerCompanionField.smaliReference}
            invoke-virtual {v0}, ${getManagerInstance.smaliReference}
            move-result-object v0
            invoke-virtual {v0, p0, p1}, ${getChatRoom.smaliReference}
            move-result-object v0
            if-eqz v0, :revanced_missing_chat_room
            invoke-virtual {v0}, ${memberSetGetter.smaliReference}
            move-result-object v0
            return-object v0
            :revanced_missing_chat_room
            const/4 v0, 0x0
            return-object v0
        """.trimIndent(),
    ),
)

private fun bridgeMethod(
    definingClass: String,
    name: String,
    parameterTypes: List<String> = emptyList(),
    returnType: String,
    accessFlags: Int = AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
    registerCount: Int,
    instructions: String,
): MutableMethod = ImmutableMethod(
    definingClass,
    name,
    parameterTypes.map { ImmutableMethodParameter(it, null, null) },
    returnType,
    accessFlags,
    null,
    null,
    MutableMethodImplementation(registerCount),
).toMutable().apply {
    addInstructionsWithLabels(0, instructions)
}

private fun wideFieldGetter(
    definingClass: String,
    name: String,
    field: FieldReference,
) = bridgeMethod(
    definingClass = definingClass,
    name = name,
    returnType = "J",
    registerCount = 3,
    instructions = """
        iget-wide v0, p0, ${field.smaliReference}
        return-wide v0
    """.trimIndent(),
)

private fun MutableMethod.profileImageTypeConstant(profileStateType: String): FieldReference {
    val profileLoadIndex = instructions.indexOfFirst { instruction ->
        instruction.getReference<MethodReference>()?.let { reference ->
            reference.definingClass == PROFILE_VIEW_CLASS &&
                reference.name == "load" &&
                reference.parameterTypeNames == listOf("J", "Ljava/lang/String;", "I") &&
                reference.returnType == "V"
        } == true
    }.takeIf { it >= 0 }
        ?: throw PatchException("Could not find the profile view load call.")

    return instructions.take(profileLoadIndex)
        .asSequence()
        .filter { it.opcode == Opcode.SGET_OBJECT }
        .mapNotNull { it.getReference<FieldReference>() }
        .lastOrNull { field ->
            field.definingClass == profileStateType && field.type == profileStateType
        }
        ?: throw PatchException("Could not resolve the open profile user status constant.")
}

private fun Method.requirePrintedWideField(label: String): FieldReference {
    val instructions = implementation?.instructions?.toList()
        ?: throw PatchException("Method $definingClass->$name has no implementation.")
    val labelIndex = instructions.indexOfFirst { instruction ->
        instruction.getReference<StringReference>()?.string == label
    }.takeIf { it >= 0 }
        ?: throw PatchException("Could not find ChatLog label: $label")
    val appendInstruction = instructions.asSequence()
        .drop(labelIndex + 1)
        .firstOrNull { instruction ->
            instruction.getReference<MethodReference>()?.let { reference ->
                reference.definingClass == "Ljava/lang/StringBuilder;" &&
                    reference.name == "append" &&
                    reference.parameterTypeNames == listOf("J") &&
                    reference.returnType == "Ljava/lang/StringBuilder;"
            } == true
        }
        ?: throw PatchException("Could not find wide append for ChatLog label: $label")
    val valueRegister = appendInstruction.invokeRegisters?.getOrNull(1)
        ?: throw PatchException("Could not resolve value register for ChatLog label: $label")

    return instructions.asSequence()
        .take(labelIndex)
        .filter { it.opcode == Opcode.IGET_WIDE }
        .filterIsInstance<TwoRegisterInstruction>()
        .filter { it.registerA == valueRegister }
        .mapNotNull { (it as Instruction).getReference<FieldReference>() }
        .lastOrNull { field ->
            field.definingClass == definingClass && field.type == "J"
        }
        ?: throw PatchException("Could not resolve ChatLog field for label: $label")
}

private fun Method.requireParameterBackedField(
    parameterIndex: Int,
    opcode: Opcode,
    expectedType: String,
): FieldReference {
    val sourceRegister = parameterRegister(parameterIndex)
    return implementation?.instructions
        ?.asSequence()
        ?.filter { it.opcode == opcode }
        ?.filterIsInstance<TwoRegisterInstruction>()
        ?.filter { it.registerA == sourceRegister }
        ?.mapNotNull { (it as Instruction).getReference<FieldReference>() }
        ?.singleOrNull { it.definingClass == definingClass && it.type == expectedType }
        ?: throw PatchException(
            "Could not resolve parameter $parameterIndex field in $definingClass->$name.",
        )
}

private val Instruction.invokeRegisters: List<Int>?
    get() = when (this) {
        is FiveRegisterInstruction ->
            listOf(registerC, registerD, registerE, registerF, registerG).take(registerCount)
        is RegisterRangeInstruction -> (startRegister until startRegister + registerCount).toList()
        else -> null
    }
