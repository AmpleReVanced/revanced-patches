package app.revanced.patches.kakaotalk.layout.keywordlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.findFieldFromToString
import app.morphe.util.findMethodFromToString
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.writeRegister
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatRoomListManagerGetInstanceFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.GetChatRoomByChannelIdFingerprint
import app.revanced.patches.kakaotalk.misc.extension.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.extension.sharedExtensionPatch
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import org.w3c.dom.Element

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/keywordlog/KeywordLogPatch;"

private const val ACTIVITY_NAME =
    "app.revanced.extension.kakaotalk.keywordlog.KeywordLogListActivity"

// Slot 6 of the app's own virtual chat room id block, which is what the removed feature used.
// The block sits just above Long.MIN_VALUE so it can never collide with a server assigned id.
private const val KEYWORD_LOG_CHAT_ROOM_ID = Long.MIN_VALUE + 6

private const val ADD_ROOM_METHOD = "patch_addKeywordLogChatRoom"

private const val HANDLE_CLICK_METHOD = "patch_handleKeywordLogClick"

private const val LOAD_PROFILE_METHOD = "patch_loadKeywordLogProfile"

private const val REFRESH_ROOM_LIST_METHOD = "patch_refreshKeywordLogChatRoom"

private val registerKeywordLogActivityPatch = resourcePatch {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        copyResources(
            "kakaotalk",
            ResourceGroup("drawable", "morphe_kakaotalk_keyword_log_profile.xml"),
        )

        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activities = application.getElementsByTagName("activity")

            for (index in 0 until activities.length) {
                val activity = activities.item(index) as? Element ?: continue
                if (activity.getAttribute("android:name") == ACTIVITY_NAME) return@use
            }

            val activity = document.createElement("activity")
            activity.setAttribute("android:name", ACTIVITY_NAME)
            activity.setAttribute("android:excludeFromRecents", "true")
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:launchMode", "singleTop")
            activity.setAttribute("android:label", "@string/title_for_keyword_log_list")
            activity.setAttribute("android:theme", "@style/Theme.Default.NoActionBar")
            application.appendChild(activity)
        }
    }
}

@Suppress("unused")
val restoreKeywordLogPatch = bytecodePatch(
    name = "Restore keyword notification log",
    description = "Restores the removed keyword notification collection view "
        + "along with its chat list entry.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(
        addSettingsTabPatch,
        addExtensionPatch,
        addResourcesPatch,
        sharedExtensionPatch,
        registerKeywordLogActivityPatch,
    )

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_restore_keyword_log",
                titleKey = "morphe_settings_patch_restore_keyword_log",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val keywordManagerClass = KeywordMatchFingerprint.originalClassDef
        val keywordManagerInstance = keywordManagerClass.fields.single {
            it.type == keywordManagerClass.type && AccessFlags.STATIC.isSet(it.accessFlags)
        }
        val highlightMethod = KeywordHighlightFingerprint.originalMethod

        val chatLogType = KeywordMatchFingerprint.originalMethod.parameters[0].toString()
        val chatLogClass = classDefBy(chatLogType)
        val chatLogToString = chatLogClass.methods.single {
            it.name == "toString" && it.parameters.isEmpty()
        }
        val messageMethod = chatLogToString.findMethodFromToString("message")
        val createdAtField = chatLogToString.findFieldFromToString("createdAt")
        val friendAccessor = chatLogClass.methods.single {
            it.parameters.isEmpty() && it.returnType == FRIEND_CLASS
        }
        val nickNameField = FriendToStringFingerprint.originalMethod.findFieldFromToString("nickName")

        val chatRoomClass = ChatRoomToStringFingerprint.originalClassDef
        val chatRoomType = chatRoomClass.type
        val lastMessageField =
            ChatRoomToStringFingerprint.originalMethod.findFieldFromToString("lastMessage")
        val lastLogIdField =
            ChatRoomToStringFingerprint.originalMethod.findFieldFromToString("lastLogId")
        val lastUpdatedAtField = chatRoomLastUpdatedAtInMillisFingerprint(chatRoomType)
            .instructionMatches[0].getFieldAccessed()
        val chatRoomConstructor = chatRoomClass.methods.single {
            it.name == "<init>" && it.parameters.size == 3 &&
                it.parameters[0].toString() == "J" && it.parameters[1].toString() == "[J"
        }
        val chatRoomTypeEnum = chatRoomConstructor.parameters[2].toString()
        val keywordLogListEnum = keywordLogListEnumFingerprint(chatRoomTypeEnum)
            .instructionMatches[1].getFieldAccessed()
        val chatRoomTypeField = chatRoomTypeField(chatRoomType, chatRoomTypeEnum)

        val chatRoomTitleMethod = chatRoomTitleUsageFingerprint(chatRoomType)
            .instructionMatches[1].getMethodCalled()
        val chatRoomDisplayNameMethod = chatRoomDisplayNameUsageFingerprint(chatRoomType)
            .instructionMatches[0].getMethodCalled()

        val managerInstanceMethod = ChatRoomListManagerGetInstanceFingerprint.originalMethod
        val chatRoomLookupMethod = GetChatRoomByChannelIdFingerprint.originalMethod
        if (chatRoomLookupMethod.returnType != chatRoomType) {
            throw PatchException("Resolved an unexpected chat room lookup method.")
        }
        val managerCompanionField =
            GetChatRoomByChannelIdFingerprint.originalClassDef.fields.single {
                AccessFlags.STATIC.isSet(it.accessFlags) &&
                    it.type == managerInstanceMethod.definingClass
            }

        val intentFingerprint = chatRoomIntentFingerprint(chatRoomTypeEnum)
        val chatRoomIntentMethod = intentFingerprint.originalMethod
        val intentUtilsCompanionType = intentFingerprint.originalClassDef.type
        val intentUtilsCompanionField =
            classDefBy(intentUtilsCompanionType.substringBeforeLast('$') + ";")
                .fields.single { it.type == intentUtilsCompanionType }

        hookKeywordMatch()

        addHelperMethod(
            EXTENSION_CLASS,
            "recordFromChatLog",
            listOf("Ljava/lang/Object;", "Z"),
            "V",
            16,
            """
                if-eqz p1, :skip
                invoke-static {}, $EXTENSION_CLASS->isEnabled()Z
                move-result v12
                if-eqz v12, :skip
                instance-of v12, p0, $chatLogType
                if-eqz v12, :skip
                check-cast p0, $chatLogType
                invoke-virtual {p0}, $chatLogType->getId()J
                move-result-wide v0
                invoke-virtual {p0}, $chatLogType->getChatRoomId()J
                move-result-wide v2
                invoke-virtual {p0}, $chatLogType->getUserId()J
                move-result-wide v4
                invoke-virtual {p0}, ${messageMethod.smaliReference}
                move-result-object v8
                const/4 v6, 0x0
                const/4 v7, 0x0
                invoke-virtual {p0}, ${friendAccessor.smaliReference}
                move-result-object v13
                if-eqz v13, :no_sender
                iget-object v6, v13, ${nickNameField.smaliReference}
                invoke-virtual {v13}, $FRIEND_CLASS->getProfileImageUrl()Ljava/lang/String;
                move-result-object v7
                :no_sender
                iget v12, p0, ${createdAtField.smaliReference}
                int-to-long v9, v12
                const-wide/16 v11, 0x3e8
                mul-long/2addr v9, v11
                invoke-static/range {v0 .. v10}, $EXTENSION_CLASS->record(JJJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V
                :skip
                return-void
            """,
        )

        addHelperMethod(
            EXTENSION_CLASS,
            "createChatRoomIntent",
            listOf("Landroid/content/Context;", "J"),
            "Landroid/content/Intent;",
            10,
            """
                sget-object v6, ${managerCompanionField.smaliReference}
                invoke-virtual {v6}, ${managerInstanceMethod.smaliReference}
                move-result-object v6
                invoke-virtual {v6, p1, p2}, ${chatRoomLookupMethod.smaliReference}
                move-result-object v6
                if-eqz v6, :unknown
                iget-object v4, v6, ${chatRoomTypeField.smaliReference}
                sget-object v0, ${intentUtilsCompanionField.smaliReference}
                move-object v1, p0
                move-wide v2, p1
                const/4 v5, 0x0
                invoke-virtual/range {v0 .. v5}, ${chatRoomIntentMethod.smaliReference}
                move-result-object v0
                return-object v0
                :unknown
                const/4 v0, 0x0
                return-object v0
            """,
        )

        addHelperMethod(
            EXTENSION_CLASS,
            "resolveChatRoomName",
            listOf("J"),
            "Ljava/lang/String;",
            8,
            """
                sget-object v0, ${managerCompanionField.smaliReference}
                invoke-virtual {v0}, ${managerInstanceMethod.smaliReference}
                move-result-object v0
                invoke-virtual {v0, p0, p1}, ${chatRoomLookupMethod.smaliReference}
                move-result-object v0
                if-eqz v0, :unknown
                invoke-virtual {v0}, ${chatRoomTitleMethod.smaliReference}
                move-result-object v0
                return-object v0
                :unknown
                const/4 v0, 0x0
                return-object v0
            """,
        )

        addHelperMethod(
            EXTENSION_CLASS,
            "highlight",
            listOf("Landroid/text/SpannableStringBuilder;"),
            "Ljava/lang/CharSequence;",
            4,
            """
                sget-object v0, ${keywordManagerInstance.smaliReference}
                invoke-virtual {v0, p0}, ${highlightMethod.smaliReference}
                move-result-object v0
                return-object v0
            """,
        )

        addPseudoChatRoom(
            chatRoomType = chatRoomType,
            chatRoomConstructor = chatRoomConstructor.smaliReference,
            keywordLogListEnum = keywordLogListEnum.smaliReference,
            lastMessageField = lastMessageField.smaliReference,
            lastLogIdField = lastLogIdField.smaliReference,
            lastUpdatedAtField = lastUpdatedAtField.smaliReference,
        )

        listOf(chatRoomTitleMethod, chatRoomDisplayNameMethod).forEach { method ->
            method.overrideKeywordLogTitle(
                chatRoomTypeField.smaliReference,
                keywordLogListEnum.smaliReference,
            )
        }

        hookChatRoomItemClick(chatRoomType)

        hookChatRoomProfile(chatRoomTypeEnum, keywordLogListEnum.smaliReference)
    }
}

/**
 * ChatRoom declares more than one field of the chat room type, so every no argument getter that
 * returns one must agree on which field is the real type before its descriptor can be baked in.
 */
private fun BytecodePatchContext.chatRoomTypeField(
    chatRoomType: String,
    chatRoomTypeEnum: String,
): MutableField {
    val fields = chatRoomTypeFieldFingerprint(chatRoomType, chatRoomTypeEnum)
        .matchAll()
        .map { it.instructionMatches[0].getFieldAccessed() }
        .distinctBy { it.name }

    return fields.singleOrNull()
        ?: throw PatchException("Chat room type getters resolve to ${fields.size} different fields.")
}

private fun BytecodePatchContext.hookKeywordMatch() {
    keywordMatchCallSiteFingerprint(KeywordMatchFingerprint.originalMethod)
        .matchAll()
        .sortedByDescending { it.instructionMatches[3].index }
        .forEach { match ->
            val callIndex = match.instructionMatches[0].index
            val resultIndex = match.instructionMatches[3].index
            val invoke = match.method.getInstruction(callIndex) as? FiveRegisterInstruction
                ?: throw PatchException("Could not infer the chat log register of the keyword match.")
            val chatLogRegister = invoke.registerD
            val resultRegister =
                match.method.getInstruction<OneRegisterInstruction>(resultIndex).registerA

            if (match.method.instructions.asSequence()
                    .drop(callIndex + 1)
                    .take(resultIndex - callIndex)
                    .any { it.writeRegister == chatLogRegister }
            ) {
                throw PatchException("The keyword match chat log does not survive its suspension.")
            }
            if (chatLogRegister > 15 || resultRegister > 15) {
                throw PatchException("The keyword match result is not held in low registers.")
            }

            match.method.addInstructions(
                resultIndex + 1,
                "invoke-static {v$chatLogRegister, v$resultRegister}, $EXTENSION_CLASS->recordFromChatLog(Ljava/lang/Object;Z)V",
            )
        }
}

private fun BytecodePatchContext.addHelperMethod(
    definingClass: String,
    name: String,
    parameters: List<String>,
    returnType: String,
    registerCount: Int,
    instructions: String,
) {
    val mutableClass = mutableClassDefBy(definingClass)
    mutableClass.methods.removeIf { it.name == name }

    mutableClass.methods.add(
        ImmutableMethod(
            definingClass,
            name,
            parameters.map { ImmutableMethodParameter(it, null, null) },
            returnType,
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(registerCount),
        ).toMutable().apply { addInstructionsWithLabels(0, instructions) },
    )
}

private fun BytecodePatchContext.addPseudoChatRoom(
    chatRoomType: String,
    chatRoomConstructor: String,
    keywordLogListEnum: String,
    lastMessageField: String,
    lastLogIdField: String,
    lastUpdatedAtField: String,
) {
    addHelperMethod(
        chatRoomType,
        ADD_ROOM_METHOD,
        listOf("Ljava/util/List;"),
        "Ljava/util/List;",
        10,
        """
            invoke-static {}, $EXTENSION_CLASS->isEnabled()Z
            move-result v0
            if-eqz v0, :skip
            new-instance v0, Ljava/util/ArrayList;
            check-cast p0, Ljava/util/Collection;
            invoke-direct {v0, p0}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V
            new-instance v1, $chatRoomType
            const-wide v2, ${KEYWORD_LOG_CHAT_ROOM_ID}L
            const/4 v4, 0x0
            new-array v4, v4, [J
            sget-object v5, $keywordLogListEnum
            invoke-direct {v1, v2, v3, v4, v5}, $chatRoomConstructor
            invoke-static {}, $EXTENSION_CLASS->roomLastMessage()Ljava/lang/String;
            move-result-object v6
            iput-object v6, v1, $lastMessageField
            invoke-static {}, $EXTENSION_CLASS->roomLastLogId()J
            move-result-wide v6
            iput-wide v6, v1, $lastLogIdField
            invoke-static {}, $EXTENSION_CLASS->roomLastUpdatedAt()I
            move-result v6
            iput v6, v1, $lastUpdatedAtField
            const/4 v6, 0x0
            invoke-virtual {v0, v6, v1}, Ljava/util/ArrayList;->add(ILjava/lang/Object;)V
            return-object v0
            :skip
            return-object p0
        """,
    )

    val filterMethod = ChatRoomListFilterFingerprint.originalMethod
    val viewModelType = classDefBy {
        it.sourceFile == "GeneralChatRoomListViewModel.kt" && !it.type.contains('$')
    }.type

    val filterCall = generalChatRoomListFilterCallFingerprint(viewModelType, filterMethod)
    val callIndex = filterCall.instructionMatches[0].index
    val resultRegister =
        filterCall.method.getInstruction<OneRegisterInstruction>(callIndex + 1).registerA
    val filterCallClass = filterCall.originalClassDef
    val viewModelField = filterCallClass.fields.singleOrNull { field ->
        field.type == viewModelType && !AccessFlags.STATIC.isSet(field.accessFlags)
    } ?: throw PatchException("Could not infer the chat room list ViewModel field.")
    val refreshMethod = chatRoomListRefreshFingerprint(viewModelType).originalMethod

    addHelperMethod(
        filterCallClass.type,
        REFRESH_ROOM_LIST_METHOD,
        listOf("Ljava/lang/Object;"),
        "V",
        2,
        """
            instance-of v0, p0, ${filterCallClass.type}
            if-eqz v0, :done
            check-cast p0, ${filterCallClass.type}
            iget-object p0, p0, ${viewModelField.smaliReference}
            invoke-virtual {p0}, ${refreshMethod.smaliReference}
            :done
            return-void
        """,
    )

    filterCall.method.addInstructions(
        callIndex + 2,
        """
            invoke-static/range {v$resultRegister .. v$resultRegister}, $chatRoomType->$ADD_ROOM_METHOD(Ljava/util/List;)Ljava/util/List;
            move-result-object v$resultRegister
            invoke-static/range {p0 .. p0}, $EXTENSION_CLASS->registerChatRoomListRefreshTarget(Ljava/lang/Object;)V
        """,
    )
}

private fun MutableMethod.overrideKeywordLogTitle(
    chatRoomTypeField: String,
    keywordLogListEnum: String,
) {
    val provider = getFreeRegisterProvider(0, 2)
    val typeRegister = provider.getFreeRegister4Bit()
    val enumRegister = provider.getFreeRegister4Bit()

    addInstructionsWithLabels(
        0,
        """
            iget-object v$typeRegister, p0, $chatRoomTypeField
            sget-object v$enumRegister, $keywordLogListEnum
            if-ne v$typeRegister, v$enumRegister, :original
            invoke-static {}, $EXTENSION_CLASS->roomTitle()Ljava/lang/String;
            move-result-object v$typeRegister
            return-object v$typeRegister
            :original
            nop
        """,
    )
}

private fun MutableMethod.returnVoidWhenHandled(
    helperDescriptor: String,
    index: Int = 0,
) {
    val provider = getFreeRegisterProvider(index, 1)
    val handledRegister = provider.getFreeRegister4Bit()

    addInstructionsWithLabels(
        index,
        """
            invoke-static {p0, p1}, $helperDescriptor
            move-result v$handledRegister
            if-eqz v$handledRegister, :original
            return-void
            :original
            nop
        """,
    )
}

private fun BytecodePatchContext.hookChatRoomProfile(
    chatRoomTypeEnum: String,
    keywordLogListEnum: String,
) {
    addHelperMethod(
        PROFILE_VIEW_CLASS,
        LOAD_PROFILE_METHOD,
        listOf(PROFILE_VIEW_CLASS, CHAT_ROOM_PROFILE_DATA_CLASS),
        "Z",
        6,
        """
            invoke-virtual {p1}, $CHAT_ROOM_PROFILE_DATA_CLASS->getType()$chatRoomTypeEnum
            move-result-object v0
            sget-object v1, $keywordLogListEnum
            if-ne v0, v1, :ignore
            invoke-static {}, $EXTENSION_CLASS->roomProfileDrawable()I
            move-result v0
            if-eqz v0, :ignore
            invoke-virtual {p0, v0}, $PROFILE_VIEW_CLASS->getXmlDrawable(I)$SQUIRCLE_DRAWABLE_CLASS
            move-result-object v0
            invoke-virtual {p0, v0}, $PROFILE_VIEW_CLASS->loadImmediately($SQUIRCLE_DRAWABLE_CLASS)V
            const/4 v0, 0x1
            return v0
            :ignore
            const/4 v0, 0x0
            return v0
        """,
    )

    val profileInitializedIndex = ChatRoomProfileFingerprint.instructionMatches[2].index + 1
    ChatRoomProfileFingerprint.method.returnVoidWhenHandled(
        "$PROFILE_VIEW_CLASS->$LOAD_PROFILE_METHOD($PROFILE_VIEW_CLASS$CHAT_ROOM_PROFILE_DATA_CLASS)Z",
        profileInitializedIndex,
    )
}

private fun BytecodePatchContext.hookChatRoomItemClick(
    chatRoomType: String,
) {
    val matches = ChatRoomItemClickFingerprint.instructionMatches
    val bindableGetter = matches[0].getMethodCalled()
    val itemType = matches[1].getInstruction<ReferenceInstruction>()
        .getReference<TypeReference>()!!.type
    val chatRoomIdGetter = matches[2].getMethodCalled()

    addHelperMethod(
        chatRoomType,
        HANDLE_CLICK_METHOD,
        listOf("Ljava/lang/Object;", "Landroid/view/View;"),
        "Z",
        8,
        """
            invoke-static {}, $EXTENSION_CLASS->isEnabled()Z
            move-result v0
            if-eqz v0, :ignore
            check-cast p0, ${bindableGetter.definingClass}
            invoke-virtual {p0}, ${bindableGetter.smaliReference}
            move-result-object v0
            instance-of v1, v0, $itemType
            if-eqz v1, :ignore
            check-cast v0, $itemType
            invoke-virtual {v0}, ${chatRoomIdGetter.smaliReference}
            move-result-wide v2
            const-wide v4, ${KEYWORD_LOG_CHAT_ROOM_ID}L
            cmp-long v0, v2, v4
            if-nez v0, :ignore
            invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;
            move-result-object v0
            invoke-static {v0}, $EXTENSION_CLASS->openList(Landroid/content/Context;)V
            const/4 v0, 0x1
            return v0
            :ignore
            const/4 v0, 0x0
            return v0
        """,
    )

    ChatRoomItemClickFingerprint.method.returnVoidWhenHandled(
        "$chatRoomType->$HANDLE_CLICK_METHOD(Ljava/lang/Object;Landroid/view/View;)Z",
    )
}