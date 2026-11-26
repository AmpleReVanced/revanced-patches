package app.revanced.patches.kakaotalk.interaction.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogItemViewHolderFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogVFieldPutBooleanFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogVFieldPutStringFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogViewHolderBindProfileFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatLogViewHolderSetupChatInfoViewFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ChatRoomSyncFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.FilterChatLogItemFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.FlushToDBChatLogFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.GetDeletedColorFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.GetHiddenColorFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ModifiedChatLogApplyFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ModifiedChatLogFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ModifyLogBuilderFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.PutDeletedMessageCacheFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.ReplaceToFeedFingerprint
import app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints.getDeletedMessageCacheFingerprint
import app.revanced.patches.kakaotalk.misc.extension.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.extension.sharedExtensionPatch
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.addKakaoTalkResources
import app.revanced.util.parameterRegister
import app.revanced.util.parameterTypeNames
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import org.w3c.dom.Element

private const val ARGB_32_MASK = 0xFFFF_FFFFL
private const val OPAQUE_ALPHA = 0xFF00_0000L
private const val MODIFIED_PROFILE_NICKNAME_METHOD = "revanced_modified_profile_nickname"
private const val MODIFIED_PROFILE_USER_ID_METHOD = "revanced_modified_profile_user_id"
private const val MODIFIED_PROFILE_IMAGE_URL_METHOD = "revanced_modified_profile_image_url"
private const val MODIFIED_PROFILE_IMAGE_TYPE_METHOD = "revanced_modified_profile_image_type"
private const val PRESERVE_MODIFIED_HISTORY_METHOD = "revanced_preserveModifiedHistory"
private const val CHAT_INFO_VIEW_CLASS = "Lcom/kakao/talk/widget/chatlog/ChatInfoView;"
private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/ShowDeletedHiddenOrEditedMessagePatch;"

internal const val MODIFIED_MESSAGE_IS_DELETED_OR_HIDDEN_METHOD = "revanced_isDeletedOrHidden"

private data class ModifiedProfileReferences(
    val getRecyclerItemReference: MethodReference,
    val getProfileUserIdReference: MethodReference,
    val getMemberStateReference: MethodReference,
    val getProfileImageReference: MethodReference,
    val getProfileStatusReference: MethodReference,
    val getProfileNicknameReference: MethodReference,
    val openProfileFieldReference: FieldReference,
)

private fun parseArgb32ToInt(input: String): Int {
    val t = input.trim().replace("_", "")
    val value = when {
        t.startsWith("#") -> parseHashColor(t)
        t.startsWith("0x", ignoreCase = true) -> t.substring(2).toLong(16)
        t.startsWith("-0x", ignoreCase = true) -> -t.substring(3).toLong(16)
        else -> t.toLong()
    }
    return (value and ARGB_32_MASK).toInt()
}

private fun parseHashColor(input: String): Long {
    val hex = input.substring(1)
    require(hex.length == 6 || hex.length == 8) {
        "Color must be #RRGGBB, #AARRGGBB, 0xAARRGGBB, or signed decimal."
    }

    val color = hex.toLong(16)
    return if (hex.length == 6) color or OPAQUE_ALPHA else color
}

private fun toSmaliIntLiteral(value: Int) =
    "0x" + (value.toLong() and ARGB_32_MASK).toString(16).padStart(8, '0')

private val registerModifiedMessageHistoryActivityPatch = patch {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activityName = "app.revanced.extension.kakaotalk.chatlog.ModifiedMessageHistoryActivity"
            val activities = application.getElementsByTagName("activity")

            for (i in 0 until activities.length) {
                val activity = activities.item(i) as? Element ?: continue
                if (activity.getAttribute("android:name") == activityName) {
                    activity.setAttribute("android:excludeFromRecents", "true")
                    activity.setAttribute("android:exported", "false")
                    activity.setAttribute("android:label", "@string/morphe_kakaotalk_chatlog_modified_history_title")
                    activity.setAttribute("android:theme", "@style/Theme.Default.NoActionBar")
                    return@use
                }
            }

            val activity = document.createElement("activity")
            activity.setAttribute("android:name", activityName)
            activity.setAttribute("android:excludeFromRecents", "true")
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:label", "@string/morphe_kakaotalk_chatlog_modified_history_title")
            activity.setAttribute("android:theme", "@style/Theme.Default.NoActionBar")
            application.appendChild(activity)
        }
    }
}

@Suppress("unused")
val showDeletedHiddenOrEditedMessagePatch = patch(
    name = "Show deleted, hidden, or edited messages",
    description = "Allows you to see deleted, hidden, and edited message history in chat logs.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch, addExtensionPatch, addResourcesPatch, sharedExtensionPatch, registerModifiedMessageHistoryActivityPatch)

    val deletedColorText by stringOption(
        key = "deletedColor",
        title = "Deleted color",
        description = "32-bit ARGB. Accepts #RRGGBB, #AARRGGBB, 0xAARRGGBB, or signed decimal.",
        default = "0xFFFF4444",
    )

    val hiddenColorText by stringOption(
        key = "hiddenColor",
        title = "Hidden color",
        description = "32-bit ARGB. Accepts #RRGGBB, #AARRGGBB, 0xAARRGGBB, or signed decimal.",
        default = "0xFF999999",
    )

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_show_modified_message_sender_profile",
                titleKey = "morphe_settings_patch_show_modified_message_sender_profile",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        addKakaoTalkResources()

        val deletedInt = parseArgb32ToInt(deletedColorText!!)
        val deletedLit = toSmaliIntLiteral(deletedInt)
        val hiddenInt = parseArgb32ToInt(hiddenColorText!!)
        val hiddenLit = toSmaliIntLiteral(hiddenInt)

        GetDeletedColorFingerprint.method.replaceInstruction(0, "const v0, $deletedLit")
        GetHiddenColorFingerprint.method.replaceInstruction(0, "const v0, $hiddenLit")

        val chatInfoViewClass = mutableClassDefBy("Lcom/kakao/talk/widget/chatlog/ChatInfoView;")

        chatInfoViewClass.fields.add(
            ImmutableField(
                chatInfoViewClass.type,
                "extension",
                "Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;",
                AccessFlags.PRIVATE.value,
                null,
                null,
                null
            ).toMutable()
        )

        val initMethod = chatInfoViewClass.methods.first { it.name == "<init>" && it.parameters.size == 3 }
        initMethod.addInstructions(
            initMethod.instructions.count() - 1,
            """
                new-instance p1, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                invoke-direct {p1, p0}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;-><init>(Lcom/kakao/talk/widget/chatlog/ChatInfoView;)V
                iput-object p1, p0, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->extension:Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
            """.trimIndent()
        )

        val getMaxHeightMethod = chatInfoViewClass.methods.first { it.name == "getMaxHeight" }
        val paddingTopIndex = getMaxHeightMethod.indexOfFirstInstructionOrThrow(
            methodCall(name = "getPaddingTop", opcodes = listOf(Opcode.INVOKE_VIRTUAL)),
        )
        getMaxHeightMethod.addInstructions(
            paddingTopIndex,
            "move-object v4, p0"
        )
        getMaxHeightMethod.addInstructionsWithLabels(
            getMaxHeightMethod.instructions.count() - 1,
            """
                iget-object v0, v4, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->extension:Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                if-eqz v0, :revanced_ext_end
                invoke-virtual {v0}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->getAdditionalHeight()I
                move-result v0
                add-int/2addr v2, v0
                :revanced_ext_end
                nop
            """.trimIndent()
        )

        val onDrawMethod = chatInfoViewClass.methods.first { it.name == "onDraw" }
        val firstInvokeSuperIdx = onDrawMethod.indexOfFirstInstructionOrThrow(Opcode.INVOKE_SUPER)
        onDrawMethod.addInstructionsWithLabels(
            firstInvokeSuperIdx + 1,
            """
                iget-object v0, p0, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->extension:Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                if-eqz v0, :cond_end
                invoke-virtual {v0, p1}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->draw(Landroid/graphics/Canvas;)V
                :cond_end
                nop
            """.trimIndent()
        )

        chatInfoViewClass.methods.add(
            ImmutableMethod(
                chatInfoViewClass.type,
                "getExtension",
                emptyList(),
                "Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;",
                AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                null,
                null,
                MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                            iget-object v0, p0, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->extension:Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                            return-object v0
                    """,
                )
            },
        )

        chatInfoViewClass.methods.add(
            bridgeMethod(
                definingClass = chatInfoViewClass.type,
                name = "onTouchEvent",
                parameterTypes = listOf("Landroid/view/MotionEvent;"),
                returnType = "Z",
                accessFlags = AccessFlags.PUBLIC.value,
                registerCount = 3,
                instructions = """
                    iget-object v0, p0, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->extension:Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                    if-eqz v0, :super
                    invoke-virtual {v0, p1}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->onTouchEvent(Landroid/view/MotionEvent;)Z
                    move-result v0
                    if-nez v0, :handled
                    :super
                    invoke-super {p0, p1}, Landroid/view/View;->onTouchEvent(Landroid/view/MotionEvent;)Z
                    move-result v0
                    :handled
                    return v0
                """.trimIndent(),
            ),
        )

        val setChatLogIdMethod = chatInfoViewClass.methods.first { it.name == "setChatLogId" }
        setChatLogIdMethod.addInstructionsWithLabels(
            setChatLogIdMethod.instructions.count() - 1,
            """
                iget-object p0, p0, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->extension:Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                if-eqz p0, :revanced_set_chat_log_id_end
                invoke-virtual {p0, p1, p2}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->setChatLogId(J)V
                :revanced_set_chat_log_id_end
                nop
            """.trimIndent()
        )

        val makeLayoutMethod = chatInfoViewClass.methods.first { it.name == "makeLayout" }
        val getUnreadPaint = makeLayoutMethod.instructions.indexOfLast { it.opcode == Opcode.IGET_OBJECT && it.getReference<FieldReference>()?.name == "unreadPaint" }
        makeLayoutMethod.instructions.slice(getUnreadPaint until getUnreadPaint + 10).first {
            it.opcode == Opcode.CONST_4 && (it as BuilderInstruction11n).narrowLiteral == 0x1
        }.let {
            makeLayoutMethod.replaceInstruction(
                it.location.index,
                BuilderInstruction11n(Opcode.CONST_4, (it as BuilderInstruction11n).registerA, 0x0)
            )
        }

        mutableClassDefBy("Lcom/kakao/talk/widget/chatlog/OthersChatInfoView;").apply {
            val getTotalWidthMethod = methods.first { it.name == "getTotalWidth" }
            val getPaddingLeftIndex = getTotalWidthMethod.indexOfFirstInstructionOrThrow(
                methodCall(name = "getPaddingLeft", opcodes = listOf(Opcode.INVOKE_VIRTUAL)),
            )
            getTotalWidthMethod.addInstructionsWithLabels(
                getPaddingLeftIndex,
                """
                    invoke-virtual {p0}, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->getExtension()Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                    move-result-object v1
                    if-eqz v1, :cond_extension_width
                    invoke-virtual {v1}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->getAdditionalWidth()I
                    move-result v1
                    invoke-static {v0, v1}, Ljava/lang/Math;->max(II)I
                    move-result v0
                    :cond_extension_width
                    nop
                """.trimIndent()
            )

            val makeRectMethod = methods.first { it.name == "makeRect" }
            val getBookmarkIconIndex = makeRectMethod.indexOfFirstInstructionOrThrow(
                methodCall(name = "getBookmarkIcon", opcodes = listOf(Opcode.INVOKE_VIRTUAL)),
            )
            makeRectMethod.replaceInstruction(
                getBookmarkIconIndex,
                "invoke-virtual {p0}, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->getExtension()Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;"
            )
            makeRectMethod.addInstructionsWithLabels(
                getBookmarkIconIndex + 1,
                """
                    move-result-object v1
                    if-eqz v1, :cond_extension_rect
                    invoke-virtual {p0}, Lcom/kakao/talk/widget/chatlog/OthersChatInfoView;->getTotalWidth()I
                    move-result v3
                    invoke-virtual {v1, v0, v3, v2}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->calculateRect(III)I
                    move-result v2
                    :cond_extension_rect
                    invoke-virtual {p0}, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->getBookmarkIcon()Landroid/graphics/Bitmap;
                """.trimIndent()
            )
        }

        mutableClassDefBy("Lcom/kakao/talk/widget/chatlog/MyChatInfoView;").apply {
            val getTotalWidthMethod = methods.first { it.name == "getTotalWidth" }
            val getPaddingLeftIndex = getTotalWidthMethod.indexOfFirstInstructionOrThrow(
                methodCall(name = "getPaddingLeft", opcodes = listOf(Opcode.INVOKE_VIRTUAL)),
            )
            getTotalWidthMethod.addInstructionsWithLabels(
                getPaddingLeftIndex,
                """
                    invoke-virtual {p0}, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->getExtension()Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                    move-result-object v1
                    if-eqz v1, :cond_extension_width
                    invoke-virtual {v1}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->getAdditionalWidth()I
                    move-result v1
                    invoke-static {v0, v1}, Ljava/lang/Math;->max(II)I
                    move-result v0
                    :cond_extension_width
                    nop
                """.trimIndent()
            )

            val makeRectMethod = methods.first { it.name == "makeRect" }
            val getDateLayoutIndex = makeRectMethod.indexOfFirstInstructionOrThrow(
                methodCall(name = "getDateLayout", opcodes = listOf(Opcode.INVOKE_VIRTUAL)),
            )
            makeRectMethod.replaceInstruction(
                getDateLayoutIndex,
                "invoke-virtual {p0}, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->getExtension()Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;"
            )
            makeRectMethod.addInstructionsWithLabels(
                getDateLayoutIndex + 1,
                """
                    move-result-object v0
                    if-eqz v0, :cond_extension_rect
                    invoke-virtual {p0}, Landroid/view/View;->getPaddingLeft()I
                    move-result v3
                    invoke-virtual {p0}, Lcom/kakao/talk/widget/chatlog/MyChatInfoView;->getTotalWidth()I
                    move-result v4
                    invoke-virtual {v0, v3, v4, v2}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->calculateRect(III)I
                    move-result v2
                    :cond_extension_rect
                    invoke-virtual {p0}, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->getDateLayout()Landroid/text/Layout;
                """.trimIndent()
            )
        }

        val chatLogVFieldClass = ChatLogVFieldPutBooleanFingerprint.classDef
        chatLogVFieldClass.let {
            val putBooleanMethod = ChatLogVFieldPutBooleanFingerprint.method
            val putStringMethod = ChatLogVFieldPutStringFingerprint.method
            val jsonField = it.instanceFields.singleOrNull { field ->
                field.type == "Lorg/json/JSONObject;"
            } ?: throw PatchException("Could not find VField JSON object")

            it.methods.addAll(
                listOf(
                    ImmutableMethod(
                        chatLogVFieldClass.type,
                        "putDeleted",
                        listOf(
                            ImmutableMethodParameter("Z", null, null)
                        ),
                        "V",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                const-string v0, "_revanced_deleted"
                                invoke-virtual {p0, v0, p1}, ${chatLogVFieldClass.type}->${putBooleanMethod.name}(Ljava/lang/String;Z)V
                                return-void
                            """,
                        )
                    },
                    ImmutableMethod(
                        chatLogVFieldClass.type,
                        "getDeleted",
                        emptyList(),
                        "Z",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                iget-object v0, p0, ${jsonField.smaliReference}
                                const-string v1, "_revanced_deleted"
                                const/4 v2, 0x0
                                invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->optBoolean(Ljava/lang/String;Z)Z
                                move-result v0
                                return v0
                            """,
                        )
                    },
                    ImmutableMethod(
                        chatLogVFieldClass.type,
                        "putHidden",
                        listOf(
                            ImmutableMethodParameter("Z", null, null)
                        ),
                        "V",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                const-string v0, "_revanced_hidden"
                                invoke-virtual {p0, v0, p1}, ${chatLogVFieldClass.type}->${putBooleanMethod.name}(Ljava/lang/String;Z)V
                                return-void
                            """,
                        )
                    },
                    ImmutableMethod(
                        chatLogVFieldClass.type,
                        "getHidden",
                        emptyList(),
                        "Z",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                iget-object v0, p0, ${jsonField.smaliReference}
                                const-string v1, "_revanced_hidden"
                                const/4 v2, 0x0
                                invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->optBoolean(Ljava/lang/String;Z)Z
                                move-result v0
                                return v0
                            """,
                        )
                    },
                    ImmutableMethod(
                        chatLogVFieldClass.type,
                        "putModifiedMessage",
                        listOf(
                            ImmutableMethodParameter("Ljava/lang/String;", null, null),
                            ImmutableMethodParameter("I", null, null),
                            ImmutableMethodParameter("Ljava/lang/String;", null, null),
                        ),
                        "V",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(6),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                const-string v0, "_revanced_modified_history"
                                invoke-static {p3, p1, p2}, Lapp/revanced/extension/kakaotalk/chatlog/ModifiedMessageHistoryExtension;->mergeModifiedHistory(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;
                                move-result-object v1
                                invoke-virtual {p0, v0, v1}, ${chatLogVFieldClass.type}->${putStringMethod.name}(Ljava/lang/String;Ljava/lang/String;)V
                                return-void
                            """,
                        )
                    },
                    ImmutableMethod(
                        chatLogVFieldClass.type,
                        "getModifiedHistory",
                        emptyList(),
                        "Ljava/lang/String;",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                iget-object v0, p0, ${jsonField.smaliReference}
                                const-string v1, "_revanced_modified_history"
                                const-string v2, ""
                                invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v0
                                return-object v0
                            """,
                        )
                    }
                )
            )
        }

        val chatLogClass = ChatLogFingerprint.classDef
        val vFieldField = chatLogClass.fields.first { it.type == chatLogVFieldClass.type }

        chatLogClass.methods.add(
            isDeletedOrHiddenMethod(
                chatLogClass.type,
                vFieldField.smaliReference,
                chatLogVFieldClass.type,
            ),
        )

        val modifiedChatLogType = ModifiedChatLogFingerprint.classDef.type
        val modifyLogBuilderMethod = ModifyLogBuilderFingerprint(chatLogClass.type).method

        val messageGetterReference = modifyLogBuilderMethod.instructions
            .asSequence()
            .mapNotNull { it.getReference<MethodReference>() }
            .filter {
                it.definingClass == chatLogClass.type &&
                        it.parameterTypeNames.isEmpty() &&
                        it.returnType == "Ljava/lang/String;"
            }
            .lastOrNull()
            ?: throw PatchException("Could not find modified message getter.")

        val modifyRevisionGetterReference = modifyLogBuilderMethod.instructions
            .asSequence()
            .mapNotNull { it.getReference<MethodReference>() }
            .filter {
                it.definingClass == chatLogVFieldClass.type &&
                        it.parameterTypeNames.isEmpty() &&
                        it.returnType == "I"
            }
            .lastOrNull()
            ?: throw PatchException("Could not find modified revision getter.")

        chatLogClass.methods.add(
            preserveModifiedHistoryMethod(
                definingClass = chatLogClass.type,
                chatLogType = chatLogClass.type,
                vFieldReference = vFieldField.smaliReference,
                vFieldType = vFieldField.type,
                messageGetterReference = messageGetterReference.smaliReference,
                modifyRevisionGetterReference = modifyRevisionGetterReference.smaliReference,
            )
        )

        ModifiedChatLogApplyFingerprint(chatLogClass.type, modifiedChatLogType)
            .matchAll(1 .. 2)
            .forEach {
                it.method.addModifiedHistoryHook(
                    chatLogClass.type,
                    modifiedChatLogType,
                )
            }

        val replaceToFeedMethod = ReplaceToFeedFingerprint.method
        replaceToFeedMethod.let {
            val flushToDBMethod = FlushToDBChatLogFingerprint(chatLogClass.type).method
            val getChatRoomByChannelIdMethod = ChatRoomSyncFingerprint.chatRoomLookup
            val managerClass = ReplaceToFeedFingerprint.classDef
            val managerField = managerClass.instanceFields.single {
                it.type == getChatRoomByChannelIdMethod.definingClass
            }
            val originalSyncMethod = ChatRoomSyncFingerprint.method
            val chatRoomIdGetter = ChatRoomSyncFingerprint.chatRoomIdGetter
            val lastChatLogGetter = ChatRoomSyncFingerprint.lastChatLogGetter
            val chatLogDaoField = managerClass.instanceFields.single { it.type == lastChatLogGetter.definingClass }

            fun latestMessageGuard(endLabel: String, value: Int, flag: Int) = """
                iget-object v$value, p0, ${chatLogDaoField.smaliReference}
                invoke-virtual {p1}, $chatRoomIdGetter
                move-result-wide p2
                invoke-interface {v$value, p2, p3}, $lastChatLogGetter
                move-result-object v$value
                if-eqz v$value, :$endLabel
                invoke-virtual {v$value}, ${chatLogClass.type}->getId()J
                move-result-wide v$value
                invoke-virtual {p1}, ${chatLogClass.type}->getId()J
                move-result-wide p2
                cmp-long v$flag, v$value, p2
                if-nez v$flag, :$endLabel
            """.trimIndent()

            val invokeVirtualInst = originalSyncMethod.instructions.last { it.opcode == Opcode.INVOKE_VIRTUAL }
            val invokeStaticInst = originalSyncMethod.instructions.last { it.opcode == Opcode.INVOKE_STATIC }

            fun preserveMessage(index: Int, deleted: Boolean): String {
                val chatLogRegister = it.parameterRegister(0)
                val wideRegister = it.parameterRegister(1)
                val provider = it.getFreeRegisterProvider(
                    index, 3, chatLogRegister - 1, chatLogRegister, wideRegister, wideRegister + 1,
                )
                val registers = List(provider.availableCount()) { provider.getFreeRegister() }.filter { register -> register < 16 }.sorted()
                val value = registers.firstOrNull { register -> register + 1 in registers }
                    ?: throw PatchException("Could not reserve a register pair for message IDs.")
                val valueHigh = value + 1
                val flag = registers.firstOrNull { register -> register != value && register != valueHigh }
                    ?: throw PatchException("Could not reserve a register for message flags.")
                val flagSetter = if (deleted) "putDeleted" else "putHidden"
                val deletedRegister = if (deleted) valueHigh else flag
                val hiddenRegister = if (deleted) flag else valueHigh
                val endLabel = if (deleted) "revanced_deleted_sync_end" else "revanced_hidden_sync_end"
                return """
                    iget-object v$value, p1, ${chatLogClass.type}->${vFieldField.name}:${vFieldField.type}
                    const/4 v$valueHigh, 0x1
                    invoke-virtual {v$value, v$valueHigh}, ${chatLogVFieldClass.type}->$flagSetter(Z)V
                    invoke-virtual {p1}, ${chatLogClass.type}->getId()J
                    move-result-wide p2
                    const/4 v$flag, 0x0
                    invoke-static {p2, p3, v$deletedRegister, v$hiddenRegister}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->updateByChatLogId(JZZ)V
                    invoke-virtual {p0, p1}, ${it.definingClass}->${flushToDBMethod.name}(${chatLogClass.type})Z
                    
                    ${latestMessageGuard(endLabel, value, flag)}
                    iget-object v$value, p0, ${managerField.smaliReference}
                    invoke-virtual {p1}, $chatRoomIdGetter
                    move-result-wide p2
                    invoke-virtual {v$value, p2, p3}, $getChatRoomByChannelIdMethod
                    move-result-object v$value
                    if-eqz v$value, :$endLabel
                    const/4 v$valueHigh, 0x1
                    invoke-virtual {v$value, p1, v$valueHigh}, ${invokeVirtualInst.getReference<MethodReference>()}
                    move-result-object v$value
                    const/4 v$valueHigh, 0x0
                    const/4 v$flag, 0x1
                    invoke-static {v$value, v$valueHigh, v$flag, v$valueHigh}, ${invokeStaticInst.getReference<MethodReference>()}
                    :$endLabel
                    return-void
                """.trimIndent()
            }

            val sgetObjectDeleteToAllIndex = it.indexOfFirstInstructionOrThrow(
                fieldAccess(name = "DELETE_TO_ALL", opcode = Opcode.SGET_OBJECT),
            )
            it.replaceInstruction(sgetObjectDeleteToAllIndex, "return-void")
            it.addInstructionsWithLabels(sgetObjectDeleteToAllIndex, preserveMessage(sgetObjectDeleteToAllIndex, deleted = true))

            val lastSgetFeedIndex = it.indexOfFirstInstructionReversedOrThrow(
                fieldAccess(name = "Feed", opcode = Opcode.SGET_OBJECT),
            )
            it.replaceInstruction(lastSgetFeedIndex, "return-void")
            it.addInstructionsWithLabels(lastSgetFeedIndex, preserveMessage(lastSgetFeedIndex, deleted = false))
        }

        val modifiedProfileReferences = ChatLogViewHolderBindProfileFingerprint.method.resolveModifiedProfileReferences(
            ChatLogItemViewHolderFingerprint.method.definingClass,
        )
        val chatLogViewHolderProfileClass = ChatLogViewHolderBindProfileFingerprint.classDef
        chatLogViewHolderProfileClass.methods.addAll(
            modifiedProfileAccessorMethods(chatLogViewHolderProfileClass.type, modifiedProfileReferences)
        )

        ChatLogViewHolderSetupChatInfoViewFingerprint.apply {
            val setModifyIndex = method.indexOfFirstInstructionOrThrow(
                methodCall("$CHAT_INFO_VIEW_CLASS->setModify(Z)V", opcode = Opcode.INVOKE_VIRTUAL),
            )
            val viewRegister = method.getInstruction<FiveRegisterInstruction>(setModifyIndex).registerC
            val chatRoomGetter = ChatLogItemViewHolderFingerprint.classDef.methods.single {
                it.parameters.isEmpty() && it.returnType == ChatRoomSyncFingerprint.chatRoomLookup.returnType
            }
            val bindModifiedChatInfo = bridgeMethod(
                definingClass = classDef.type,
                name = "revanced_bindModifiedChatInfo",
                parameterTypes = listOf(CHAT_INFO_VIEW_CLASS),
                returnType = "V",
                accessFlags = AccessFlags.PRIVATE.value,
                registerCount = 10,
                instructions = """
                    invoke-virtual {p1}, $CHAT_INFO_VIEW_CLASS->getExtension()Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                    move-result-object v0
                    if-eqz v0, :done

                    invoke-virtual {p0}, ${ChatLogItemViewHolderFingerprint.method.smaliReference}
                    move-result-object v1
                    instance-of v2, v1, ${chatLogClass.type}
                    if-eqz v2, :empty
                    check-cast v1, ${chatLogClass.type}
                    iget-object v2, v1, ${vFieldField.smaliReference}

                    invoke-virtual {v2}, ${vFieldField.type}->getDeleted()Z
                    move-result v3
                    invoke-virtual {v2}, ${vFieldField.type}->getHidden()Z
                    move-result v4
                    invoke-virtual {v2}, ${vFieldField.type}->getModifiedHistory()Ljava/lang/String;
                    move-result-object v5
                    invoke-virtual {v1}, ${messageGetterReference.smaliReference}
                    move-result-object v6
                    goto :bind

                    :empty
                    const/4 v3, 0x0
                    const/4 v4, 0x0
                    const-string v5, ""
                    move-object v6, v5

                    :bind
                    invoke-virtual {p0}, ${chatRoomGetter.smaliReference}
                    move-result-object v7
                    invoke-virtual {v0, v7}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->setChatRoom(Ljava/lang/Object;)V
                    invoke-virtual {v0, v3}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->setDeleted(Z)V
                    invoke-virtual {v0, v4}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->setHidden(Z)V
                    instance-of v2, p1, Lcom/kakao/talk/widget/chatlog/MyChatInfoView;
                    invoke-static {p0, v5, v6, v2}, Lapp/revanced/extension/kakaotalk/chatlog/ModifiedMessageHistoryExtension;->bindModifiedLabel(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Z)V

                    :done
                    return-void
                """.trimIndent(),
            )

            classDef.methods.add(bindModifiedChatInfo)
            method.addInstruction(
                setModifyIndex + 1,
                "invoke-direct {p0, v$viewRegister}, ${bindModifiedChatInfo.smaliReference}",
            )
        }

        FilterChatLogItemFingerprint.method.returnEarly(true)

        PutDeletedMessageCacheFingerprint.method.returnEarly()

        getDeletedMessageCacheFingerprint(chatLogClass.type).method.returnEarly(false)
    }
}

private fun MutableMethod.addModifiedHistoryHook(
    chatLogType: String,
    modifiedChatLogType: String,
) {
    val newChatLogIndex = indexOfFirstInstructionOrThrow {
        val reference = getReference<MethodReference>() ?: return@indexOfFirstInstructionOrThrow false

        reference.parameterTypeNames == listOf(chatLogType, modifiedChatLogType, "Ljava/lang/String;") &&
                reference.returnType == chatLogType
    }

    val newChatLogRegister = (getInstruction(newChatLogIndex + 1) as? OneRegisterInstruction)?.registerA
        ?: throw PatchException("Could not find modified ChatLog result register.")

    val originalChatLogRegister = parameterRegister(0)
    val preserveHistoryRegisterProvider = getFreeRegisterProvider(
        newChatLogIndex + 2,
        2,
        originalChatLogRegister,
        newChatLogRegister,
    )
    val originalChatLogArgumentRegister = preserveHistoryRegisterProvider.getFreeRegister()
    val newChatLogArgumentRegister = preserveHistoryRegisterProvider.getFreeRegister()
    if (originalChatLogArgumentRegister >= 16 || newChatLogArgumentRegister >= 16) {
        throw PatchException("Could not reserve low free registers to preserve modified message history.")
    }

    addInstructions(
        newChatLogIndex + 2,
        """
            move-object/from16 v$originalChatLogArgumentRegister, p1
            move-object/from16 v$newChatLogArgumentRegister, v$newChatLogRegister
            invoke-static {v$originalChatLogArgumentRegister, v$newChatLogArgumentRegister}, $chatLogType->$PRESERVE_MODIFIED_HISTORY_METHOD($chatLogType$chatLogType)V
        """.trimIndent(),
    )
}

private fun isDeletedOrHiddenMethod(
    definingClass: String,
    vFieldReference: String,
    vFieldType: String,
): MutableMethod = ImmutableMethod(
    definingClass,
    MODIFIED_MESSAGE_IS_DELETED_OR_HIDDEN_METHOD,
    emptyList(),
    "Z",
    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
    null,
    null,
    MutableMethodImplementation(4),
).toMutable().apply {
    // The adapter's instance keeps its flag stale until reloaded, so also check the id cache. Read
    // the id first, while p0 still holds the chat log, so later register reuse cannot clobber it.
    addInstructionsWithLabels(
        0,
        """
            invoke-virtual {p0}, $definingClass->getId()J
            move-result-wide v0
            iget-object v2, p0, $vFieldReference
            if-eqz v2, :revanced_check_cache
            invoke-virtual {v2}, $vFieldType->getDeleted()Z
            move-result v3
            if-nez v3, :revanced_modified
            invoke-virtual {v2}, $vFieldType->getHidden()Z
            move-result v3
            if-nez v3, :revanced_modified
            :revanced_check_cache
            invoke-static {v0, v1}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->isDeletedOrHiddenById(J)Z
            move-result v0
            return v0
            :revanced_modified
            const/4 v0, 0x1
            return v0
        """.trimIndent(),
    )
}

private fun preserveModifiedHistoryMethod(
    definingClass: String,
    chatLogType: String,
    vFieldReference: String,
    vFieldType: String,
    messageGetterReference: String,
    modifyRevisionGetterReference: String,
): MutableMethod = ImmutableMethod(
    definingClass,
    PRESERVE_MODIFIED_HISTORY_METHOD,
    listOf(
        ImmutableMethodParameter(chatLogType, null, null),
        ImmutableMethodParameter(chatLogType, null, null),
    ),
    "V",
    AccessFlags.PUBLIC.value or AccessFlags.STATIC.value or AccessFlags.FINAL.value,
    null,
    null,
    MutableMethodImplementation(6),
).toMutable().apply {
    addInstructions(
        0,
        """
            invoke-virtual {p0}, $messageGetterReference
            move-result-object v0
            iget-object v1, p0, $vFieldReference
            invoke-virtual {v1}, $vFieldType->getModifiedHistory()Ljava/lang/String;
            move-result-object v1
            iget-object v2, p0, $vFieldReference
            invoke-virtual {v2}, $modifyRevisionGetterReference
            move-result v2
            iget-object v3, p1, $vFieldReference
            invoke-virtual {v3, v0, v2, v1}, $vFieldType->putModifiedMessage(Ljava/lang/String;ILjava/lang/String;)V
            return-void
        """.trimIndent(),
    )
}

private fun MutableMethod.resolveModifiedProfileReferences(
    viewHolderBaseClass: String,
): ModifiedProfileReferences {
    val profileLoadIndex = instructions.indexOfFirst { instruction ->
        instruction.getReference<MethodReference>()?.isProfileLoadCall() == true
    }.takeIf { it >= 0 }
        ?: throw PatchException("Could not find modified message profile load call.")

    val profileImageIndex = (profileLoadIndex - 1 downTo 0).firstOrNull { index ->
        instructions[index].getReference<MethodReference>()?.let { reference ->
            reference.parameterTypeNames.isEmpty() &&
                    reference.returnType == "Ljava/lang/String;"
        } == true
    } ?: throw PatchException("Could not find modified message profile image getter.")

    val profileImageReference = instructions[profileImageIndex].getReference<MethodReference>()
        ?: throw PatchException("Could not resolve modified message profile image getter.")
    val memberStateType = profileImageReference.definingClass

    val getMemberStateReference = instructions.take(profileImageIndex)
        .mapNotNull { it.getReference<MethodReference>() }
        .lastOrNull {
            it.parameterTypeNames.isEmpty() &&
                    it.returnType == memberStateType
        } ?: throw PatchException("Could not find modified message member state getter.")
    val recyclerItemType = getMemberStateReference.definingClass

    val getRecyclerItemReference = instructions.take(profileImageIndex)
        .mapNotNull { it.getReference<MethodReference>() }
        .lastOrNull {
            it.definingClass == viewHolderBaseClass &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == recyclerItemType
        } ?: throw PatchException("Could not find modified message recycler item getter.")

    val getProfileUserIdReference = instructions.take(profileLoadIndex)
        .mapNotNull { it.getReference<MethodReference>() }
        .lastOrNull {
            it.definingClass == recyclerItemType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == "J"
        } ?: throw PatchException("Could not find modified message profile user id getter.")

    val openProfileFieldReference = instructions.take(profileLoadIndex)
        .mapNotNull { it.getReference<FieldReference>() }
        .lastOrNull {
            it.definingClass == it.type &&
                    it.type.startsWith("L")
        } ?: throw PatchException("Could not find modified message open profile field.")

    val getProfileStatusReference = instructions.take(profileLoadIndex)
        .mapNotNull { it.getReference<MethodReference>() }
        .lastOrNull {
            it.definingClass == memberStateType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == openProfileFieldReference.type
        } ?: throw PatchException("Could not find modified message profile status getter.")

    val nicknameSetTextIndex = instructions.indices.lastOrNull { index ->
        index > profileLoadIndex &&
                instructions[index].getReference<MethodReference>()?.isTextSetCall() == true
    } ?: throw PatchException("Could not find modified message profile nickname binding.")

    val getProfileNicknameReference = instructions.take(nicknameSetTextIndex)
        .mapNotNull { it.getReference<MethodReference>() }
        .lastOrNull {
            it.definingClass == memberStateType &&
                    it.parameterTypeNames.isEmpty() &&
                    it.returnType == "Ljava/lang/String;"
        } ?: throw PatchException("Could not find modified message profile nickname getter.")

    return ModifiedProfileReferences(
        getRecyclerItemReference,
        getProfileUserIdReference,
        getMemberStateReference,
        profileImageReference,
        getProfileStatusReference,
        getProfileNicknameReference,
        openProfileFieldReference,
    )
}

private fun MethodReference.isProfileLoadCall() =
    definingClass == "Lcom/kakao/talk/widget/ProfileView;" &&
            name == "load" &&
            parameterTypeNames == listOf("J", "Ljava/lang/String;", "I") &&
            returnType == "V"

private fun MethodReference.isTextSetCall() =
    definingClass == "Landroid/widget/TextView;" &&
            name == "setText" &&
            parameterTypeNames == listOf("Ljava/lang/CharSequence;") &&
            returnType == "V"

private fun modifiedProfileAccessorMethods(
    definingClass: String,
    references: ModifiedProfileReferences,
): List<MutableMethod> = listOf(
    ImmutableMethod(
        definingClass,
        MODIFIED_PROFILE_NICKNAME_METHOD,
        emptyList(),
        "Ljava/lang/String;",
        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
        null,
        null,
        MutableMethodImplementation(3),
    ).toMutable().apply {
        addInstructions(
            0,
            """
                invoke-virtual {p0}, ${references.getRecyclerItemReference.smaliReference}
                move-result-object v0
                invoke-virtual {v0}, ${references.getMemberStateReference.smaliReference}
                move-result-object v0
                invoke-virtual {v0}, ${references.getProfileNicknameReference.smaliReference}
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
    },
    ImmutableMethod(
        definingClass,
        MODIFIED_PROFILE_USER_ID_METHOD,
        emptyList(),
        "J",
        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
        null,
        null,
        MutableMethodImplementation(3),
    ).toMutable().apply {
        addInstructions(
            0,
            """
                invoke-virtual {p0}, ${references.getRecyclerItemReference.smaliReference}
                move-result-object v0
                invoke-virtual {v0}, ${references.getProfileUserIdReference.smaliReference}
                move-result-wide v0
                return-wide v0
            """.trimIndent(),
        )
    },
    ImmutableMethod(
        definingClass,
        MODIFIED_PROFILE_IMAGE_URL_METHOD,
        emptyList(),
        "Ljava/lang/String;",
        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
        null,
        null,
        MutableMethodImplementation(3),
    ).toMutable().apply {
        addInstructions(
            0,
            """
                invoke-virtual {p0}, ${references.getRecyclerItemReference.smaliReference}
                move-result-object v0
                invoke-virtual {v0}, ${references.getMemberStateReference.smaliReference}
                move-result-object v0
                invoke-virtual {v0}, ${references.getProfileImageReference.smaliReference}
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
    },
    ImmutableMethod(
        definingClass,
        MODIFIED_PROFILE_IMAGE_TYPE_METHOD,
        emptyList(),
        "I",
        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
        null,
        null,
        MutableMethodImplementation(3),
    ).toMutable().apply {
        addInstructions(
            0,
            """
                invoke-virtual {p0}, ${references.getRecyclerItemReference.smaliReference}
                move-result-object v0
                invoke-virtual {v0}, ${references.getMemberStateReference.smaliReference}
                move-result-object v0
                invoke-virtual {v0}, ${references.getProfileStatusReference.smaliReference}
                move-result-object v0
                sget-object v1, ${references.openProfileFieldReference.smaliReference}
                if-ne v0, v1, :revanced_modified_profile_not_open
                const/4 v0, -0x1
                return v0
                :revanced_modified_profile_not_open
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
    },
)
