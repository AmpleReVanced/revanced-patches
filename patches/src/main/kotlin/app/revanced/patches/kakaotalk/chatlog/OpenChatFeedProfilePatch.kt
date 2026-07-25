package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogViewHolderOpenLinkMemberIntentFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatMessageFeedViewHolderBindFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatMessageFeedViewHolderConfigureFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.FeedTypeLeaveGenerateMessageFingerprint
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.parameterTypeNames
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val CLICKABLE_SPAN_CLASS =
    "Lapp/revanced/extension/kakaotalk/chatlog/OpenChatFeedProfileClickableSpan;"
private const val BIND_FEED_PROFILE_CLICK_METHOD = "revanced_bindOpenChatFeedProfileClick"
private const val OPEN_FEED_PROFILE_METHOD = "revanced_openOpenChatFeedProfile"

private data class OpenChatFeedProfileReferences(
    val chatLogType: String,
    val chatRoomGetter: MethodReference,
    val chatLogMessageGetter: MethodReference,
    val openChatRoomCheck: MethodReference,
    val feedTypeCompanionField: FieldReference,
    val getFeedType: MethodReference,
    val leaveFeedTypeField: FieldReference,
    val openLinkJoinFeedTypeField: FieldReference,
    val feedMemberCompanionField: FieldReference,
    val parseFeedMember: MethodReference,
    val parseFeedMembers: MethodReference,
    val feedMemberUserIdGetter: MethodReference,
    val feedMemberNicknameGetter: MethodReference,
    val chatRoomMemberManagerGetter: MethodReference,
    val memberResolver: MethodReference,
    val friendDisplayNameGetter: MethodReference,
    val friendOpenProfileCheck: MethodReference,
    val openLinkProfileFirstCheck: MethodReference,
    val openLinkProfileSecondCheck: MethodReference,
    val friendOpenProfileIdGetter: MethodReference,
    val openLinkProfileMemberCheck: MethodReference,
    val openLinkChatInfoGetter: MethodReference,
    val openLinkChatInfoCheck: MethodReference,
    val chatIdGetter: MethodReference,
    val openLinkFacadeGetter: MethodReference,
    val openLinkManagerGetter: MethodReference,
    val openLinkIdGetter: MethodReference,
    val openLinkGetter: MethodReference,
    val openProfileFriendDataConstructor: MethodReference,
    val openProfileIntentFactory: MethodReference,
    val openProfileEntryPoint: String,
    val openLinkChatMemberIntent: MethodReference,
    val feedTextViewField: FieldReference,
)

@Suppress("unused")
val openChatFeedProfilePatch = bytecodePatch(
    name = "Open profile from open chat feed",
    description = "Opens open chat member profiles from join and leave feed messages.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch)

    execute {
        val bindMethod = ChatMessageFeedViewHolderBindFingerprint.method
        val configureMethod = ChatMessageFeedViewHolderConfigureFingerprint.method
        val references = resolveOpenChatFeedProfileReferences(
            classDefBy = { type -> mutableClassDefBy(type) },
            bindMethod = bindMethod,
            configureMethod = configureMethod,
            feedTypeLeaveMethod = FeedTypeLeaveGenerateMessageFingerprint.method,
            openLinkMemberIntentMethod = ChatLogViewHolderOpenLinkMemberIntentFingerprint.method,
        )

        val viewHolderClass = ChatMessageFeedViewHolderBindFingerprint.classDef
        viewHolderClass.methods.add(bindOpenChatFeedProfileClickMethod(viewHolderClass.type, references))
        viewHolderClass.methods.add(openOpenChatFeedProfileMethod(viewHolderClass.type, references))

        val configureCallIndex = bindMethod.instructions.indexOfFirst { instruction ->
            val reference = instruction.getReference<MethodReference>()

            (instruction.opcode == Opcode.INVOKE_VIRTUAL ||
                    instruction.opcode == Opcode.INVOKE_VIRTUAL_RANGE) &&
                    reference?.definingClass == bindMethod.definingClass &&
                    reference.name == configureMethod.name &&
                    reference.parameterTypeNames == listOf(references.chatLogType) &&
                    reference.returnType == "V"
        }.takeIf { it >= 0 }
            ?: throw PatchException("Could not find chat message feed configuration call.")
        val configureCallRegisters = bindMethod.chatMessageFeedConfigureCallRegisters(configureCallIndex)
        val bindCallRegisterProvider = bindMethod.getFreeRegisterProvider(
            configureCallIndex + 1,
            3,
            configureCallRegisters.receiver,
            configureCallRegisters.chatLog,
        )
        val receiverRegister = bindCallRegisterProvider.getFreeRegister4Bit()
        val chatLogRegister = bindCallRegisterProvider.getFreeRegister4Bit()
        val textViewRegister = bindCallRegisterProvider.getFreeRegister4Bit()

        bindMethod.addInstructions(
            configureCallIndex + 1,
            """
                move-object/from16 v$receiverRegister, v${configureCallRegisters.receiver}
                iget-object v$textViewRegister, v$receiverRegister, ${references.feedTextViewField.smaliReference}
                move-object/from16 v$chatLogRegister, v${configureCallRegisters.chatLog}
                invoke-direct {v$receiverRegister, v$chatLogRegister, v$textViewRegister}, ${viewHolderClass.type}->$BIND_FEED_PROFILE_CLICK_METHOD(${references.chatLogType}Landroid/widget/TextView;)V
            """.trimIndent(),
        )
    }
}

private data class FeedConfigureCallRegisters(
    val receiver: Int,
    val chatLog: Int,
)

private fun MutableMethod.chatMessageFeedConfigureCallRegisters(index: Int): FeedConfigureCallRegisters {
    val instruction = getInstruction(index)

    return when (instruction) {
        is BuilderInstruction35c -> {
            if (instruction.registerCount != 2) {
                throw PatchException("Unexpected chat message feed configuration register count.")
            }
            FeedConfigureCallRegisters(
                receiver = instruction.registerC,
                chatLog = instruction.registerD,
            )
        }

        is BuilderInstruction3rc -> {
            if (instruction.registerCount != 2) {
                throw PatchException("Unexpected chat message feed configuration register range count.")
            }
            FeedConfigureCallRegisters(
                receiver = instruction.startRegister,
                chatLog = instruction.startRegister + 1,
            )
        }

        else -> throw PatchException("Unsupported chat message feed configuration invoke instruction.")
    }
}

private fun bindOpenChatFeedProfileClickMethod(
    definingClass: String,
    references: OpenChatFeedProfileReferences,
): MutableMethod = ImmutableMethod(
    definingClass,
    BIND_FEED_PROFILE_CLICK_METHOD,
    listOf(
        ImmutableMethodParameter(references.chatLogType, null, null),
        ImmutableMethodParameter("Landroid/widget/TextView;", null, null),
    ),
    "V",
    AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
    null,
    null,
    MutableMethodImplementation(15),
).toMutable().apply {
    addInstructionsWithLabels(
        0,
        """
            invoke-virtual {p0}, ${references.chatRoomGetter.smaliReference}
            move-result-object v0
            invoke-static {v0}, ${references.openChatRoomCheck.smaliReference}
            move-result v0
            if-eqz v0, :revanced_feed_profile_end

            :revanced_feed_profile_try_start
            new-instance v0, Lorg/json/JSONObject;
            invoke-virtual {p1}, ${references.chatLogMessageGetter.smaliReference}
            move-result-object v1
            invoke-direct {v0, v1}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V

            sget-object v1, ${references.feedTypeCompanionField.smaliReference}
            const-string v2, "feedType"
            const/4 v3, -0x1
            invoke-virtual {v0, v2, v3}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;I)I
            move-result v2
            invoke-virtual {v1, v2}, ${references.getFeedType.smaliReference}
            move-result-object v1

            sget-object v2, ${references.openLinkJoinFeedTypeField.smaliReference}
            if-ne v1, v2, :revanced_feed_profile_leave

            const-string v1, "members"
            invoke-virtual {v0, v1}, Lorg/json/JSONObject;->optJSONArray(Ljava/lang/String;)Lorg/json/JSONArray;
            move-result-object v0
            if-eqz v0, :revanced_feed_profile_try_end
            sget-object v1, ${references.feedMemberCompanionField.smaliReference}
            invoke-virtual {v1, v0}, ${references.parseFeedMembers.smaliReference}
            move-result-object v0
            invoke-interface {v0}, Ljava/util/List;->size()I
            move-result v1
            const/4 v2, 0x1
            if-ne v1, v2, :revanced_feed_profile_try_end
            const/4 v1, 0x0
            invoke-interface {v0, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;
            move-result-object v0
            check-cast v0, ${references.feedMemberCompanionField.definingClass}
            goto :revanced_feed_profile_resolve_member

            :revanced_feed_profile_leave
            sget-object v2, ${references.leaveFeedTypeField.smaliReference}
            if-ne v1, v2, :revanced_feed_profile_try_end
            const-string v1, "member"
            invoke-virtual {v0, v1}, Lorg/json/JSONObject;->optJSONObject(Ljava/lang/String;)Lorg/json/JSONObject;
            move-result-object v0
            if-eqz v0, :revanced_feed_profile_try_end
            sget-object v1, ${references.feedMemberCompanionField.smaliReference}
            invoke-virtual {v1, v0}, ${references.parseFeedMember.smaliReference}
            move-result-object v0

            :revanced_feed_profile_resolve_member
            invoke-virtual {v0}, ${references.feedMemberNicknameGetter.smaliReference}
            move-result-object v8
            invoke-virtual {p0}, ${references.chatRoomGetter.smaliReference}
            move-result-object v1
            invoke-virtual {v1}, ${references.chatRoomMemberManagerGetter.smaliReference}
            move-result-object v2
            invoke-virtual {v0}, ${references.feedMemberUserIdGetter.smaliReference}
            move-result-wide v3
            invoke-virtual {p0}, ${references.chatRoomGetter.smaliReference}
            move-result-object v1
            invoke-virtual {v1}, ${references.openLinkIdGetter.smaliReference}
            move-result-wide v5
            move-object v7, v0
            invoke-virtual/range {v2 .. v7}, ${references.memberResolver.smaliReference}
            move-result-object v0
            if-eqz v0, :revanced_feed_profile_try_end

            invoke-virtual {v0}, ${references.friendDisplayNameGetter.smaliReference}
            move-result-object v9
            invoke-virtual {p2}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;
            move-result-object v10
            if-eqz v10, :revanced_feed_profile_bind_raw_name
            if-eqz v9, :revanced_feed_profile_bind_raw_name
            invoke-interface {v10}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;
            move-result-object v10
            invoke-virtual {v10, v9}, Ljava/lang/String;->indexOf(Ljava/lang/String;)I
            move-result v11
            if-ltz v11, :revanced_feed_profile_bind_raw_name
            invoke-static {p2, v9, p0, v0}, $CLICKABLE_SPAN_CLASS->bind(Landroid/widget/TextView;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V
            goto :revanced_feed_profile_try_end

            :revanced_feed_profile_bind_raw_name
            invoke-static {p2, v8, p0, v0}, $CLICKABLE_SPAN_CLASS->bind(Landroid/widget/TextView;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

            :revanced_feed_profile_try_end
            goto :revanced_feed_profile_end

            :revanced_feed_profile_catch
            move-exception v0

            :revanced_feed_profile_end
            return-void
        """.trimIndent(),
    )
    addFeedProfileParseCatch()
}

private fun MutableMethod.addFeedProfileParseCatch() {
    // addInstructionsWithLabels does not keep .catch directives, so create the dex try block explicitly.
    val implementation = implementation
        ?: throw PatchException("Missing feed profile binding method implementation.")
    val tryStartIndex = instructions.indexOfFirst { instruction ->
        instruction.opcode == Opcode.NEW_INSTANCE &&
                instruction.getReference<TypeReference>()?.type == "Lorg/json/JSONObject;"
    }.takeIf { it >= 0 }
        ?: throw PatchException("Could not find feed profile parse try start.")
    val handlerIndex = instructions.indexOfFirst { it.opcode == Opcode.MOVE_EXCEPTION }
        .takeIf { it >= 0 }
        ?: throw PatchException("Could not find feed profile parse catch handler.")
    val tryEndIndex = handlerIndex - 1
    if (tryEndIndex < tryStartIndex || instructions[tryEndIndex].opcode != Opcode.GOTO) {
        throw PatchException("Could not find feed profile parse try end.")
    }

    implementation.addCatch(
        "Ljava/lang/Exception;",
        implementation.newLabelForIndex(tryStartIndex),
        implementation.newLabelForIndex(tryEndIndex),
        implementation.newLabelForIndex(handlerIndex),
    )
}

private fun openOpenChatFeedProfileMethod(
    definingClass: String,
    references: OpenChatFeedProfileReferences,
): MutableMethod = ImmutableMethod(
    definingClass,
    OPEN_FEED_PROFILE_METHOD,
    listOf(
        ImmutableMethodParameter("Ljava/lang/Object;", null, null),
        ImmutableMethodParameter("Landroid/view/View;", null, null),
    ),
    "V",
    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
    null,
    null,
    MutableMethodImplementation(14),
).toMutable().apply {
    addInstructionsWithLabels(
        0,
        """
            check-cast p1, ${references.openLinkChatMemberIntent.parameterTypeNames[1]}
            invoke-virtual {p1}, ${references.friendOpenProfileCheck.smaliReference}
            move-result v0
            if-eqz v0, :revanced_open_profile_link_check
            const/4 v2, 0x0
            goto :revanced_open_profile_open_profile

            :revanced_open_profile_link_check
            invoke-static {}, ${references.openLinkManagerGetter.smaliReference}
            move-result-object v0
            invoke-virtual {p0}, ${references.chatRoomGetter.smaliReference}
            move-result-object v1
            invoke-virtual {v1}, ${references.openLinkIdGetter.smaliReference}
            move-result-wide v1
            invoke-interface {v0, v1, v2}, ${references.openLinkGetter.smaliReference}
            move-result-object v1
            if-eqz v1, :revanced_open_profile_generic
            invoke-virtual {v1}, ${references.openLinkProfileFirstCheck.smaliReference}
            move-result v0
            if-eqz v0, :revanced_open_profile_generic
            invoke-virtual {v1}, ${references.openLinkProfileSecondCheck.smaliReference}
            move-result v0
            if-eqz v0, :revanced_open_profile_generic
            invoke-static {}, ${references.openLinkManagerGetter.smaliReference}
            move-result-object v0
            invoke-virtual {p1}, ${references.friendOpenProfileIdGetter.smaliReference}
            move-result-wide v3
            invoke-interface {v0, v1, v3, v4}, ${references.openLinkProfileMemberCheck.smaliReference}
            move-result v0
            if-eqz v0, :revanced_open_profile_generic
            move-object v2, v1

            :revanced_open_profile_open_profile
            invoke-static {}, ${references.openLinkFacadeGetter.smaliReference}
            move-result-object v0
            invoke-virtual {p2}, Landroid/view/View;->getContext()Landroid/content/Context;
            move-result-object v1
            new-instance v3, ${references.openProfileFriendDataConstructor.definingClass}
            invoke-virtual {p0}, ${references.chatRoomGetter.smaliReference}
            move-result-object v4
            invoke-virtual {v4}, ${references.chatIdGetter.smaliReference}
            move-result-wide v4
            invoke-direct {v3, v4, v5, p1}, ${references.openProfileFriendDataConstructor.smaliReference}
            const-string v4, "${references.openProfileEntryPoint}"
            const/4 v5, 0x0
            const/16 v6, 0x10
            const/4 v7, 0x0
            invoke-static/range {v0 .. v7}, ${references.openProfileIntentFactory.smaliReference}
            move-result-object v0
            goto :revanced_open_profile_intent

            :revanced_open_profile_generic
            invoke-static {}, ${references.openLinkFacadeGetter.smaliReference}
            move-result-object v0
            invoke-virtual {p2}, Landroid/view/View;->getContext()Landroid/content/Context;
            move-result-object v1
            move-object v2, p1
            invoke-virtual {p0}, ${references.chatRoomGetter.smaliReference}
            move-result-object v3
            invoke-virtual {v3}, ${references.openLinkChatInfoGetter.smaliReference}
            move-result-object v3
            invoke-static {v3}, ${references.openLinkChatInfoCheck.smaliReference}
            move-result v3
            invoke-virtual {p0}, ${references.chatRoomGetter.smaliReference}
            move-result-object v4
            invoke-virtual {v4}, ${references.chatIdGetter.smaliReference}
            move-result-wide v4
            invoke-static {}, ${references.openLinkManagerGetter.smaliReference}
            move-result-object v6
            invoke-virtual {p0}, ${references.chatRoomGetter.smaliReference}
            move-result-object v7
            invoke-virtual {v7}, ${references.openLinkIdGetter.smaliReference}
            move-result-wide v7
            invoke-interface {v6, v7, v8}, ${references.openLinkGetter.smaliReference}
            move-result-object v6
            new-instance v7, Ljava/util/HashMap;
            invoke-direct {v7}, Ljava/util/HashMap;-><init>()V
            invoke-interface/range {v0 .. v7}, ${references.openLinkChatMemberIntent.smaliReference}
            move-result-object v0

            :revanced_open_profile_intent
            if-eqz v0, :revanced_open_profile_end

            invoke-virtual {p2}, Landroid/view/View;->getContext()Landroid/content/Context;
            move-result-object v1
            invoke-virtual {v1, v0}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V

            :revanced_open_profile_end
            return-void
        """.trimIndent(),
    )
}

private fun resolveOpenChatFeedProfileReferences(
    classDefBy: (String) -> ClassDef,
    bindMethod: Method,
    configureMethod: Method,
    feedTypeLeaveMethod: Method,
    openLinkMemberIntentMethod: Method,
): OpenChatFeedProfileReferences {
    val chatLogType = configureMethod.parameterTypeNames.singleOrNull()
        ?: throw PatchException("Could not infer feed chat log type.")
    val bindInstructions = bindMethod.instructions.toList()
    val bindMethodReferences = bindInstructions.mapNotNull { it.getReference<MethodReference>() }
    val configureInstructions = configureMethod.instructions.toList()
    val configureMethodReferences = configureInstructions.mapNotNull { it.getReference<MethodReference>() }
    val configureFieldReferences = configureInstructions.mapNotNull { it.getReference<FieldReference>() }
    val feedTypeLeaveMethodReferences = feedTypeLeaveMethod.instructions
        .mapNotNull { it.getReference<MethodReference>() }
    val openLinkMemberIntentInstructions = openLinkMemberIntentMethod.instructions.toList()
    val openLinkMemberIntentMethodReferences = openLinkMemberIntentInstructions
        .mapNotNull { it.getReference<MethodReference>() }

    val feedMessageReference = bindMethodReferences
        .firstOrNull {
            it.returnType == "Ljava/lang/CharSequence;" &&
                    it.parameterTypeNames.lastOrNull() == chatLogType
        } ?: throw PatchException("Could not find feed message formatter.")
    val chatRoomType = feedMessageReference.parameterTypeNames.first()
    val chatRoomGetter = bindMethodReferences
        .lastOrNull {
            it.definingClass != feedMessageReference.definingClass &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == chatRoomType
        } ?: throw PatchException("Could not find feed chat room getter.")

    val linkMovementIndex = bindInstructions.indexOfFirst { instruction ->
        instruction.getReference<MethodReference>()?.let { reference ->
            reference.definingClass == "Landroid/text/method/LinkMovementMethod;" &&
                    reference.name == "getInstance"
        } == true
    }.takeIf { it >= 0 }
        ?: throw PatchException("Could not find feed text movement method setup.")
    val feedTextViewField = bindInstructions.take(linkMovementIndex)
        .mapNotNull { it.getReference<FieldReference>() }
        .lastOrNull { it.type == "Landroid/widget/TextView;" }
        ?: throw PatchException("Could not find feed message TextView field.")

    val chatLogMessageGetter = configureMethodReferences
        .firstOrNull {
            it.definingClass == chatLogType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == "Ljava/lang/String;"
        } ?: throw PatchException("Could not find feed JSON getter.")

    val getFeedType = configureMethodReferences
        .firstOrNull {
            it.parameterTypeNames == listOf("I") &&
                    it.returnType.startsWith("L")
        } ?: throw PatchException("Could not find feed type resolver.")
    val feedTypeCompanionField = configureFieldReferences
        .firstOrNull {
            it.definingClass == getFeedType.returnType &&
                    it.type == getFeedType.definingClass
        } ?: throw PatchException("Could not find feed type companion field.")
    val feedTypeClass = classDefBy(getFeedType.returnType)
    val leaveFeedTypeField = feedTypeClass.findEnumField("LEAVE")
    val openLinkJoinFeedTypeField = feedTypeClass.findEnumField("OPENLINK_JOIN")

    val parseFeedMembers = configureMethodReferences
        .firstOrNull {
            it.parameterTypeNames == listOf("Lorg/json/JSONArray;") &&
                    it.returnType == "Ljava/util/List;"
        } ?: throw PatchException("Could not find feed members parser.")
    val feedMemberCompanionField = configureFieldReferences
        .firstOrNull { it.type == parseFeedMembers.definingClass }
        ?: throw PatchException("Could not find feed member companion field.")
    val feedMemberType = feedMemberCompanionField.definingClass
    val parseFeedMember = classDefBy(parseFeedMembers.definingClass).methods
        .firstOrNull {
            it.parameterTypeNames == listOf("Lorg/json/JSONObject;") &&
                    it.returnType == feedMemberType
        } ?: throw PatchException("Could not find feed member parser.")
    val feedMemberUserIdGetter = configureMethodReferences
        .firstOrNull {
            it.definingClass == feedMemberType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == "J"
        } ?: throw PatchException("Could not find feed member user id getter.")
    val feedMemberNicknameGetter = feedTypeLeaveMethodReferences
        .firstOrNull {
            it.definingClass == feedMemberType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == "Ljava/lang/String;"
        } ?: throw PatchException("Could not find feed member nickname getter.")

    val memberResolver = feedTypeLeaveMethodReferences
        .firstOrNull {
            it.parameterTypeNames == listOf("J", "J", feedMemberType) &&
                    it.returnType == "Lcom/kakao/talk/db/model/Friend;"
        } ?: throw PatchException("Could not find open chat feed member resolver.")
    val friendDisplayNameGetter = feedTypeLeaveMethodReferences
        .firstOrNull {
            it.definingClass == memberResolver.returnType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == "Ljava/lang/String;"
        } ?: throw PatchException("Could not find open chat member display name getter.")
    val chatRoomMemberManagerGetter = feedTypeLeaveMethodReferences
        .firstOrNull {
            it.definingClass == chatRoomType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == memberResolver.definingClass
        } ?: throw PatchException("Could not find chat room member manager getter.")
    val openChatRoomCheck = feedTypeLeaveMethodReferences
        .firstOrNull {
            it.parameterTypeNames == listOf(chatRoomType) &&
                    it.returnType == "Z"
        } ?: throw PatchException("Could not find open chat room check.")

    val openLinkChatMemberIntent = openLinkMemberIntentMethodReferences
        .firstOrNull {
            it.returnType == "Landroid/content/Intent;" &&
                    it.parameterTypeNames == listOf(
                        "Landroid/content/Context;",
                        "Lcom/kakao/talk/db/model/Friend;",
                        "Z",
                        "J",
                        "Lcom/kakao/talk/openlink/db/model/OpenLink;",
                        "Ljava/util/HashMap;",
                    )
        } ?: throw PatchException("Could not find open link chat member intent builder.")
    val openLinkGetter = openLinkMemberIntentMethodReferences
        .firstOrNull {
            it.parameterTypeNames == listOf("J") &&
                    it.returnType == "Lcom/kakao/talk/openlink/db/model/OpenLink;"
        } ?: throw PatchException("Could not find open link getter.")
    val openLinkManagerGetter = openLinkMemberIntentMethodReferences
        .firstOrNull {
            it.parameterTypeNames.isEmpty() &&
                    it.returnType == openLinkGetter.definingClass
        } ?: throw PatchException("Could not find open link manager getter.")
    val openLinkFacadeGetter = openLinkMemberIntentMethodReferences
        .firstOrNull {
            it.parameterTypeNames.isEmpty() &&
                    it.returnType == openLinkChatMemberIntent.definingClass
        } ?: throw PatchException("Could not find open link facade getter.")
    val friendType = openLinkChatMemberIntent.parameterTypeNames[1]
    val openLinkViewHolderClass = classDefBy(openLinkMemberIntentMethod.definingClass)
    val openProfileMemberIntentMethod = openLinkViewHolderClass.methods
        .firstOrNull { method ->
            if (method.name == openLinkMemberIntentMethod.name ||
                method.parameterTypeNames != openLinkMemberIntentMethod.parameterTypeNames ||
                method.returnType != openLinkMemberIntentMethod.returnType
            ) {
                return@firstOrNull false
            }

            val references = method.instructions.mapNotNull { it.getReference<MethodReference>() }
            references.any {
                it.definingClass == openLinkMemberIntentMethod.definingClass &&
                        it.name == openLinkMemberIntentMethod.name &&
                        it.parameterTypeNames == openLinkMemberIntentMethod.parameterTypeNames
            } && references.any {
                it.definingClass == friendType &&
                        it.parameterTypeNames.isEmpty() &&
                        it.returnType == "Z"
            }
        } ?: throw PatchException("Could not find open profile member intent branch.")
    val openProfileMemberIntentReferences = openProfileMemberIntentMethod.instructions
        .mapNotNull { it.getReference<MethodReference>() }
    val friendOpenProfileCheck = openProfileMemberIntentReferences
        .firstOrNull {
            it.definingClass == friendType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == "Z"
        } ?: throw PatchException("Could not find open profile member check.")
    val openProfileFriendDataConstructor = openProfileMemberIntentReferences
        .firstOrNull {
            it.name == "<init>" &&
                    it.parameterTypeNames == listOf("J", friendType) &&
                    it.returnType == "V"
        } ?: throw PatchException("Could not find open profile friend data constructor.")
    val openProfileIntentFactory = openProfileMemberIntentReferences
        .firstOrNull {
            it.returnType == "Landroid/content/Intent;" &&
                    it.parameterTypeNames.size == 8 &&
                    it.parameterTypeNames[0] == openLinkChatMemberIntent.definingClass &&
                    it.parameterTypeNames[1] == "Landroid/content/Context;" &&
                    it.parameterTypeNames[2] == openLinkGetter.returnType &&
                    it.parameterTypeNames[3] == openProfileFriendDataConstructor.definingClass &&
                    it.parameterTypeNames[4] == "Ljava/lang/String;" &&
                    it.parameterTypeNames[6] == "I"
        } ?: throw PatchException("Could not find open profile intent factory.")
    val openProfileEntryPoint = openProfileMemberIntentMethod.instructions
        .mapNotNull { it.getReference<StringReference>()?.string }
        .firstOrNull { it.startsWith("C") }
        ?: throw PatchException("Could not find open profile entry point.")
    val openLinkProfileBranchReference = openLinkMemberIntentMethodReferences
        .firstOrNull {
            it.definingClass == openLinkMemberIntentMethod.definingClass &&
                    it.parameterTypeNames == listOf(openLinkGetter.returnType, friendType) &&
                    it.returnType == "Z"
        } ?: throw PatchException("Could not find open link profile branch check.")
    val openLinkProfileBranchMethod = openLinkViewHolderClass.methods
        .firstOrNull {
            it.name == openLinkProfileBranchReference.name &&
                    it.parameterTypeNames == openLinkProfileBranchReference.parameterTypeNames &&
                    it.returnType == openLinkProfileBranchReference.returnType
        } ?: throw PatchException("Could not resolve open link profile branch check.")
    val openLinkProfileBranchReferences = openLinkProfileBranchMethod.instructions
        .mapNotNull { it.getReference<MethodReference>() }
    val openLinkProfileChecks = openLinkProfileBranchReferences
        .filter {
            it.definingClass == openLinkGetter.returnType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == "Z"
        }
        .take(2)
    if (openLinkProfileChecks.size != 2) {
        throw PatchException("Could not find open link profile checks.")
    }
    val friendOpenProfileIdGetter = openLinkProfileBranchReferences
        .firstOrNull {
            it.definingClass == friendType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == "J"
        } ?: throw PatchException("Could not find open profile member id getter.")
    val openLinkProfileMemberCheck = openLinkProfileBranchReferences
        .firstOrNull {
            it.definingClass == openLinkManagerGetter.returnType &&
                    it.parameterTypeNames == listOf(openLinkGetter.returnType, "J") &&
                    it.returnType == "Z"
        } ?: throw PatchException("Could not find open link profile member check.")

    val openLinkMemberIntentIndex = openLinkMemberIntentInstructions.indexOfFirst { instruction ->
        instruction.getReference<MethodReference>()?.let { reference ->
            reference.definingClass == openLinkChatMemberIntent.definingClass &&
                    reference.name == openLinkChatMemberIntent.name &&
                    reference.parameterTypeNames == openLinkChatMemberIntent.parameterTypeNames
        } == true
    }.takeIf { it >= 0 }
        ?: throw PatchException("Could not find open link chat member intent call.")
    val openLinkManagerGetterIndex = openLinkMemberIntentInstructions
        .take(openLinkMemberIntentIndex)
        .indexOfLast { instruction ->
            instruction.getReference<MethodReference>()?.let { reference ->
                reference.definingClass == openLinkManagerGetter.definingClass &&
                        reference.name == openLinkManagerGetter.name &&
                        reference.parameterTypeNames == openLinkManagerGetter.parameterTypeNames
            } == true
        }.takeIf { it >= 0 }
        ?: throw PatchException("Could not find open link manager getter call.")
    val openLinkFacadeGetterIndex = openLinkMemberIntentInstructions
        .take(openLinkManagerGetterIndex)
        .indexOfLast { instruction ->
            instruction.getReference<MethodReference>()?.let { reference ->
                reference.definingClass == openLinkFacadeGetter.definingClass &&
                        reference.name == openLinkFacadeGetter.name &&
                        reference.parameterTypeNames == openLinkFacadeGetter.parameterTypeNames
            } == true
        }.takeIf { it >= 0 }
        ?: throw PatchException("Could not find open link facade getter call.")
    val openLinkMemberIntentArgumentReferences = openLinkMemberIntentInstructions
        .subList(openLinkFacadeGetterIndex, openLinkManagerGetterIndex)
        .mapNotNull { it.getReference<MethodReference>() }
    val openLinkChatInfoGetter = openLinkMemberIntentArgumentReferences
        .firstOrNull { reference ->
            reference.definingClass == chatRoomType &&
                    reference.parameterTypeNames.isEmpty() &&
                    reference.returnType.startsWith("L") &&
                    openLinkMemberIntentArgumentReferences.any {
                        it.parameterTypeNames == listOf(reference.returnType) &&
                                it.returnType == "Z"
                    }
        } ?: throw PatchException("Could not find open link chat info getter.")
    val openLinkChatInfoCheck = openLinkMemberIntentArgumentReferences
        .firstOrNull {
            it.parameterTypeNames == listOf(openLinkChatInfoGetter.returnType) &&
                    it.returnType == "Z"
        } ?: throw PatchException("Could not find open link chat info check.")
    val chatIdGetter = openLinkMemberIntentArgumentReferences
        .firstOrNull {
            it.definingClass == chatRoomType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == "J"
        } ?: throw PatchException("Could not find open link chat id getter.")
    val openLinkIdGetter = openLinkMemberIntentInstructions
        .subList(openLinkManagerGetterIndex, openLinkMemberIntentIndex)
        .mapNotNull { it.getReference<MethodReference>() }
        .firstOrNull {
            it.definingClass == chatRoomType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == "J"
        } ?: throw PatchException("Could not find open link id getter.")

    return OpenChatFeedProfileReferences(
        chatLogType = chatLogType,
        chatRoomGetter = chatRoomGetter,
        chatLogMessageGetter = chatLogMessageGetter,
        openChatRoomCheck = openChatRoomCheck,
        feedTypeCompanionField = feedTypeCompanionField,
        getFeedType = getFeedType,
        leaveFeedTypeField = leaveFeedTypeField,
        openLinkJoinFeedTypeField = openLinkJoinFeedTypeField,
        feedMemberCompanionField = feedMemberCompanionField,
        parseFeedMember = parseFeedMember,
        parseFeedMembers = parseFeedMembers,
        feedMemberUserIdGetter = feedMemberUserIdGetter,
        feedMemberNicknameGetter = feedMemberNicknameGetter,
        chatRoomMemberManagerGetter = chatRoomMemberManagerGetter,
        memberResolver = memberResolver,
        friendDisplayNameGetter = friendDisplayNameGetter,
        friendOpenProfileCheck = friendOpenProfileCheck,
        openLinkProfileFirstCheck = openLinkProfileChecks[0],
        openLinkProfileSecondCheck = openLinkProfileChecks[1],
        friendOpenProfileIdGetter = friendOpenProfileIdGetter,
        openLinkProfileMemberCheck = openLinkProfileMemberCheck,
        openLinkChatInfoGetter = openLinkChatInfoGetter,
        openLinkChatInfoCheck = openLinkChatInfoCheck,
        chatIdGetter = chatIdGetter,
        openLinkFacadeGetter = openLinkFacadeGetter,
        openLinkManagerGetter = openLinkManagerGetter,
        openLinkIdGetter = openLinkIdGetter,
        openLinkGetter = openLinkGetter,
        openProfileFriendDataConstructor = openProfileFriendDataConstructor,
        openProfileIntentFactory = openProfileIntentFactory,
        openProfileEntryPoint = openProfileEntryPoint,
        openLinkChatMemberIntent = openLinkChatMemberIntent,
        feedTextViewField = feedTextViewField,
    )
}

private fun ClassDef.findEnumField(name: String): FieldReference =
    fields.firstOrNull { field ->
        field.name == name && field.type == type
    } ?: throw PatchException("Could not find feed type enum field $name.")
