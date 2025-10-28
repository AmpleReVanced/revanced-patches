package app.revanced.patches.kakaotalk.chatlog

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.kakaotalk.chatlog.fingerprints.chatInfoViewClassFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.chatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.chatLogItemViewHolderFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.chatLogVFieldPutBooleanFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.chatLogViewHolderSetupChatInfoViewFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.checkViewableChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.filterChatLogItemFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.flushToDBChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.getDeletedMessageCacheFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.myChatInfoViewClassFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.othersChatInfoViewClassFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.putDeletedMessageCacheFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.replaceToFeedFingerprint
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.sharedExtensionPatch
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Suppress("unused")
val showDeletedOrHiddenMessagePatch = bytecodePatch(
    name = "Show deleted or hidden messages",
    description = "Allows you to see deleted/hidden messages in chat logs.",
) {
    compatibleWith("com.kakao.talk"("25.9.0"))
    dependsOn(addExtensionPatch, addResourcesPatch, sharedExtensionPatch)

    execute {
        addResources("kakaotalk", "chatlog.showDeletedOrHiddenMessagePatch")

        val chatInfoViewClass = chatInfoViewClassFingerprint.classDef

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
        getMaxHeightMethod.addInstructionsWithLabels(
            getMaxHeightMethod.instructions.count() - 1,
            """
                iget-object v0, p0, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->extension:Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;
                if-eqz v0, :cond_extension_end
                invoke-virtual {v0}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->getAdditionalHeight()I
                move-result v0
                add-int/2addr v2, v0
                :cond_extension_end
                nop
            """.trimIndent()
        )

        val onDrawMethod = chatInfoViewClass.methods.first { it.name == "onDraw" }
        onDrawMethod.replaceInstruction(
            onDrawMethod.instructions.last { it.opcode == Opcode.RETURN_VOID }.location.index,
            "iget-object v0, p0, Lcom/kakao/talk/widget/chatlog/ChatInfoView;->extension:Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;"
        )
        onDrawMethod.addInstructionsWithLabels(
            onDrawMethod.instructions.size,
            """
                if-eqz v0, :cond_end
                invoke-virtual {v0, p1}, Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;->draw(Landroid/graphics/Canvas;)V
                :cond_end
                return-void
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

        val otherChatInfoViewClass = othersChatInfoViewClassFingerprint.classDef
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

        val myChatInfoViewClass = myChatInfoViewClassFingerprint.classDef
        myChatInfoViewClass.let {
            val getTotalWidthMethod = myChatInfoViewClass.methods.first { it.name == "getTotalWidth" }
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

            val makeRectMethod = myChatInfoViewClass.methods.first { it.name == "makeRect" }
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

        val chatLogVFieldClass = chatLogVFieldPutBooleanFingerprint.classDef
        chatLogVFieldClass.let {
            val putBooleanMethod = chatLogVFieldPutBooleanFingerprint.method

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

        val chatLogClass = chatLogFingerprint.classDef
        val vFieldField = chatLogClass.fields.first { it.type == chatLogVFieldClass.type }

        val replaceToFeedMethod = replaceToFeedFingerprint.method
        replaceToFeedMethod.let {
            val flushToDBMethod = flushToDBChatLogFingerprint.method

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
                    return-void
                """.trimIndent()
            )
        }

        val checkViewableChatLogMethod = checkViewableChatLogFingerprint.method
        checkViewableChatLogMethod.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )

        val chatLogViewHolderSetupChatInfoViewMethod = chatLogViewHolderSetupChatInfoViewFingerprint.method
        chatLogViewHolderSetupChatInfoViewMethod.let {
            val getChatLogItemMethod = chatLogItemViewHolderFingerprint.method

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

        filterChatLogItemFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )

        putDeletedMessageCacheFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )

        getDeletedMessageCacheFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent()
        )
    }
}
