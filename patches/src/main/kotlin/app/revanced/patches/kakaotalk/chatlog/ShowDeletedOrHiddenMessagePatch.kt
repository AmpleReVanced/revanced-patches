package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.all.misc.resources.addAppResources
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatInfoViewClassFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogItemViewHolderFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogVFieldPutBooleanFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogViewHolderSetupChatInfoViewFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatRoomListManagerGetInstanceFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.CheckViewableChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.FilterChatLogItemFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.FlushToDBChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.GetChatRoomByChannelIdFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.GetDeletedColorFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.GetDeletedMessageCacheFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.GetHiddenColorFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.MyChatInfoViewClassFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.OriginalSyncMethodFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.OthersChatInfoViewClassFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.PutDeletedMessageCacheFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ReplaceToFeedFingerprint
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private fun parseArgb32ToInt(input: String): Int {
    val t = input.trim().replace("_", "")
    val value = when {
        t.startsWith("0x", ignoreCase = true) -> t.substring(2).toLong(16)
        t.startsWith("-0x", ignoreCase = true) -> -t.substring(3).toLong(16)
        else -> t.toLong()
    }
    return (value and 0xFFFF_FFFFL).toInt()
}

private fun toSmaliIntLiteral(v: Int): String {
    return if (v < 0) {
        val absHex = kotlin.math.abs(v.toLong()).toString(16)
        "-0x$absHex"
    } else {
        "0x" + v.toString(16)
    }
}

@Suppress("unused")
val showDeletedOrHiddenMessagePatch = bytecodePatch(
    name = "Show deleted or hidden messages",
    description = "Allows you to see deleted/hidden messages in chat logs.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch, addResourcesPatch, sharedExtensionPatch)

    val deletedColorText by stringOption(
        key = "deletedColor",
        title = "Deleted color",
        description = "32-bit ARGB. Accepts 0xAARRGGBB or signed decimal.",
        default = "0xFFFF4444",
    )

    val hiddenColorText by stringOption(
        key = "hiddenColor",
        title = "Hidden color",
        description = "32-bit ARGB. Accepts 0xAARRGGBB or signed decimal.",
        default = "0xFF999999",
    )

    execute {
        addAppResources("kakaotalk")

        val deletedInt = parseArgb32ToInt(deletedColorText!!)
        val deletedLit = toSmaliIntLiteral(deletedInt)
        val hiddenInt = parseArgb32ToInt(hiddenColorText!!)
        val hiddenLit = toSmaliIntLiteral(hiddenInt)

        GetDeletedColorFingerprint.method.replaceInstruction(0, "const v0, $deletedLit")
        GetHiddenColorFingerprint.method.replaceInstruction(0, "const v0, $hiddenLit")

        val chatInfoViewClass = ChatInfoViewClassFingerprint.classDef

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
        val paddingTopIndex = getMaxHeightMethod.instructions.indexOfFirst {
            it.opcode == Opcode.INVOKE_VIRTUAL &&
                    it.getReference<MethodReference>()?.name == "getPaddingTop"
        }
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
        val firstInvokeSuperIdx = onDrawMethod.instructions.indexOfFirst { it.opcode == Opcode.INVOKE_SUPER }
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
                    """
                            iget-object v0, p0, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->extension:Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                            return-object v0
                    """,
                )
            },
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

        val otherChatInfoViewClass = OthersChatInfoViewClassFingerprint.classDef
        otherChatInfoViewClass.let {
            val getTotalWidthMethod = otherChatInfoViewClass.methods.first { it.name == "getTotalWidth" }
            val getPaddingLeftIndex = getTotalWidthMethod.instructions.first {
                it.opcode == Opcode.INVOKE_VIRTUAL && it.getReference<MethodReference>()?.name == "getPaddingLeft"
            }.location.index
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

            val makeRectMethod = otherChatInfoViewClass.methods.first { it.name == "makeRect" }
            val getBookmarkIconIndex = makeRectMethod.instructions.first {
                it.opcode == Opcode.INVOKE_VIRTUAL && it.getReference<MethodReference>()?.name == "getBookmarkIcon"
            }.location.index
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

        MyChatInfoViewClassFingerprint.classDef.let {
            val getTotalWidthMethod = it.methods.first { it.name == "getTotalWidth" }
            val getPaddingLeftIndex = getTotalWidthMethod.instructions.first {
                it.opcode == Opcode.INVOKE_VIRTUAL && it.getReference<MethodReference>()?.name == "getPaddingLeft"
            }.location.index
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

            val makeRectMethod =
                MyChatInfoViewClassFingerprint.classDef.methods.first { it.name == "makeRect" }
            val getDateLayoutIndex = makeRectMethod.instructions.first {
                it.opcode == Opcode.INVOKE_VIRTUAL && it.getReference<MethodReference>()?.name == "getDateLayout"
            }.location.index
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
                            """
                                iget-object v0, p0, ${chatLogVFieldClass.type}->a:Lorg/json/JSONObject;
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
                            """
                                iget-object v0, p0, ${chatLogVFieldClass.type}->a:Lorg/json/JSONObject;
                                const-string v1, "_revanced_hidden"
                                const/4 v2, 0x0
                                invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->optBoolean(Ljava/lang/String;Z)Z
                                move-result v0
                                return v0
                            """,
                        )
                    }
                )
            )
        }

        val chatLogClass = ChatLogFingerprint.classDef
        val vFieldField = chatLogClass.fields.first { it.type == chatLogVFieldClass.type }

        val replaceToFeedMethod = ReplaceToFeedFingerprint.method
        replaceToFeedMethod.let {
            val flushToDBMethod = FlushToDBChatLogFingerprint.method
            val chatRoomListManagerGetInstanceMethod = ChatRoomListManagerGetInstanceFingerprint.method
            val getChatRoomByChannelIdMethod = GetChatRoomByChannelIdFingerprint.method
            val originalSyncMethod = OriginalSyncMethodFingerprint.method

            val invokeVirtualInst = originalSyncMethod.instructions.last { it.opcode == Opcode.INVOKE_VIRTUAL }
            val invokeStaticInst = originalSyncMethod.instructions.last { it.opcode == Opcode.INVOKE_STATIC }

            val sgetObjectDeleteToAllIndex = it.instructions.indexOfFirst { it.opcode == Opcode.SGET_OBJECT && it.getReference<FieldReference>()?.name == "DELETE_TO_ALL" }
            it.replaceInstruction(
                sgetObjectDeleteToAllIndex,
                "nop"
            )

            it.addInstructions(
                sgetObjectDeleteToAllIndex + 1,
                """
                    iget-object v0, p1, ${chatLogClass.type}->${vFieldField.name}:${vFieldField.type}
                    const/4 v1, 0x1
                    invoke-virtual {v0, v1}, ${chatLogVFieldClass.type}->putDeleted(Z)V
                    invoke-virtual {p0, p1}, ${it.definingClass}->${flushToDBMethod.name}(${chatLogClass.type})Z
                    
                    sget-object v0, ${chatRoomListManagerGetInstanceMethod.returnType}->o:${chatRoomListManagerGetInstanceMethod.definingClass}
                    invoke-virtual {v0}, $chatRoomListManagerGetInstanceMethod
                    move-result-object v0
                    invoke-virtual {v0, v3, v4}, $getChatRoomByChannelIdMethod
                    move-result-object v0
                    const/4 v1, 0x1
                    invoke-virtual {v0, p1, v1}, ${invokeVirtualInst.getReference<MethodReference>()}
                    move-result-object v0
                    const/4 v1, 0x0
                    const/4 v2, 0x1
                    invoke-static {v0, v1, v2, v1}, ${invokeStaticInst.getReference<MethodReference>()}
                    return-void
                """.trimIndent()
            )

            val lastSgetFeedIndex = it.instructions.indexOfLast { it.opcode == Opcode.SGET_OBJECT && it.getReference<FieldReference>()?.name == "Feed" }
            it.replaceInstruction(
                lastSgetFeedIndex,
                "nop"
            )

            it.addInstructions(
                lastSgetFeedIndex + 1,
                """
                    iget-object v0, p1, ${chatLogClass.type}->${vFieldField.name}:${vFieldField.type}
                    const/4 v1, 0x1
                    invoke-virtual {v0, v1}, ${chatLogVFieldClass.type}->putHidden(Z)V
                    invoke-virtual {p0, p1}, ${it.definingClass}->${flushToDBMethod.name}(${chatLogClass.type})Z
                    
                    sget-object v0, ${chatRoomListManagerGetInstanceMethod.returnType}->o:${chatRoomListManagerGetInstanceMethod.definingClass}
                    invoke-virtual {v0}, $chatRoomListManagerGetInstanceMethod
                    move-result-object v0
                    invoke-virtual {v0, v3, v4}, $getChatRoomByChannelIdMethod
                    move-result-object v0
                    const/4 v1, 0x1
                    invoke-virtual {v0, p1, v1}, ${invokeVirtualInst.getReference<MethodReference>()}
                    move-result-object v0
                    const/4 v1, 0x0
                    const/4 v2, 0x1
                    invoke-static {v0, v1, v2, v1}, ${invokeStaticInst.getReference<MethodReference>()}
                    return-void
                """.trimIndent()
            )
        }

        CheckViewableChatLogFingerprint.method.returnEarly(true)

        val chatLogViewHolderSetupChatInfoViewMethod = ChatLogViewHolderSetupChatInfoViewFingerprint.method
        chatLogViewHolderSetupChatInfoViewMethod.let {
            val getChatLogItemMethod = ChatLogItemViewHolderFingerprint.method

            val setModifyIndex = it.instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_VIRTUAL &&
                        it.getReference<MethodReference>()?.name == "setModify"
            }

            it.addInstructionsWithLabels(
                setModifyIndex + 1,
                """
                    invoke-virtual {v0}, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->getExtension()Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                    move-result-object v5
                    if-eqz v5, :skip_set_flags
                    
                    invoke-virtual {p0}, $getChatLogItemMethod
                    move-result-object v6
                    instance-of v7, v6, ${chatLogClass.type}
                    if-eqz v7, :cond_chatlog_null
                    check-cast v6, ${chatLogClass.type}
                    goto :goto_chatlog_cvar
                    :cond_chatlog_null
                    const/4 v6, 0x0
                    :goto_chatlog_cvar
                    if-nez v6, :cond_get_vfield
                    const/4 v8, 0x0
                    const/4 v9, 0x0
                    goto :goto_set_flags
                    
                    :cond_get_vfield
                    iget-object v7, v6, ${chatLogClass.type}->${vFieldField.name}:${vFieldField.type}
                    
                    invoke-virtual {v7}, ${vFieldField.type}->getDeleted()Z
                    move-result v8
                    
                    invoke-virtual {v7}, ${vFieldField.type}->getHidden()Z
                    move-result v9
                    
                    :goto_set_flags
                    invoke-virtual {v5, v8}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->setDeleted(Z)V
                    invoke-virtual {v5, v9}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->setHidden(Z)V
                    
                    :skip_set_flags
                    nop
                """.trimIndent()
            )
        }

        FilterChatLogItemFingerprint.method.returnEarly(true)

        PutDeletedMessageCacheFingerprint.method.returnEarly()

        GetDeletedMessageCacheFingerprint.method.returnEarly(false)
    }
}
