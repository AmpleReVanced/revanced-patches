package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatInfoViewClassFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.MyChatInfoViewClassFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.OthersChatInfoViewClassFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

context(patchContext: BytecodePatchContext)
internal fun hookChatInfoViewExtension() {
    val chatInfoViewClass = ChatInfoViewClassFingerprint.classDef

    chatInfoViewClass.fields.add(
        ImmutableField(
            chatInfoViewClass.type,
            "extension",
            CHAT_INFO_EXTENSION_CLASS,
            AccessFlags.PRIVATE.value,
            null,
            null,
            null,
        ).toMutable(),
    )

    val initMethod = chatInfoViewClass.methods.first { it.name == "<init>" && it.parameters.size == 3 }
    initMethod.addInstructions(
        initMethod.instructions.count() - 1,
        """
            new-instance p1, $CHAT_INFO_EXTENSION_CLASS
            invoke-direct {p1, p0}, $CHAT_INFO_EXTENSION_CLASS-><init>($CHAT_INFO_VIEW_CLASS)V
            iput-object p1, p0, $CHAT_INFO_VIEW_CLASS->extension:$CHAT_INFO_EXTENSION_CLASS
        """.trimIndent(),
    )

    hookChatInfoViewHeight()
    hookChatInfoViewDraw()
    addChatInfoViewExtensionAccessor(chatInfoViewClass.type)
    allowChatInfoUnreadTextToShrink()
    hookOtherChatInfoViewGeometry()
    hookMyChatInfoViewGeometry()
}

context(patchContext: BytecodePatchContext)
private fun hookChatInfoViewHeight() {
    val getMaxHeightMethod = ChatInfoViewClassFingerprint.classDef.methods.first { it.name == "getMaxHeight" }
    val paddingTopIndex = getMaxHeightMethod.indexOfFirstInstructionOrThrow {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "getPaddingTop"
    }

    getMaxHeightMethod.addInstructions(paddingTopIndex, "move-object v4, p0")
    getMaxHeightMethod.addInstructionsWithLabels(
        getMaxHeightMethod.instructions.count() - 1,
        """
            iget-object v0, v4, $CHAT_INFO_VIEW_CLASS->extension:$CHAT_INFO_EXTENSION_CLASS
            if-eqz v0, :revanced_ext_end
            invoke-virtual {v0}, $CHAT_INFO_EXTENSION_CLASS->getAdditionalHeight()I
            move-result v0
            add-int/2addr v2, v0
            :revanced_ext_end
            nop
        """.trimIndent(),
    )
}

context(patchContext: BytecodePatchContext)
private fun hookChatInfoViewDraw() {
    val onDrawMethod = ChatInfoViewClassFingerprint.classDef.methods.first { it.name == "onDraw" }
    val firstInvokeSuperIndex = onDrawMethod.indexOfFirstInstructionOrThrow(Opcode.INVOKE_SUPER)

    onDrawMethod.addInstructionsWithLabels(
        firstInvokeSuperIndex + 1,
        """
            iget-object v0, p0, $CHAT_INFO_VIEW_CLASS->extension:$CHAT_INFO_EXTENSION_CLASS
            if-eqz v0, :cond_end
            invoke-virtual {v0, p1}, $CHAT_INFO_EXTENSION_CLASS->draw(Landroid/graphics/Canvas;)V
            :cond_end
            nop
        """.trimIndent(),
    )
}

context(patchContext: BytecodePatchContext)
private fun addChatInfoViewExtensionAccessor(chatInfoViewClassType: String) {
    ChatInfoViewClassFingerprint.classDef.methods.add(
        ImmutableMethod(
            chatInfoViewClassType,
            "getExtension",
            emptyList(),
            CHAT_INFO_EXTENSION_CLASS,
            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
            null,
            null,
            MutableMethodImplementation(3),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    iget-object v0, p0, $CHAT_INFO_VIEW_CLASS->extension:$CHAT_INFO_EXTENSION_CLASS
                    return-object v0
                """.trimIndent(),
            )
        },
    )
}

context(patchContext: BytecodePatchContext)
private fun allowChatInfoUnreadTextToShrink() {
    val makeLayoutMethod = ChatInfoViewClassFingerprint.classDef.methods.first { it.name == "makeLayout" }
    val getUnreadPaintIndex = makeLayoutMethod.instructions.indexOfLast {
        it.opcode == Opcode.IGET_OBJECT &&
                it.getReference<FieldReference>()?.name == "unreadPaint"
    }
    val fixedWidthInstruction = makeLayoutMethod.instructions
        .slice(getUnreadPaintIndex until getUnreadPaintIndex + 10)
        .first {
            it.opcode == Opcode.CONST_4 &&
                    (it as BuilderInstruction11n).narrowLiteral == 0x1
        } as BuilderInstruction11n

    makeLayoutMethod.replaceInstruction(
        fixedWidthInstruction.location.index,
        BuilderInstruction11n(Opcode.CONST_4, fixedWidthInstruction.registerA, 0x0),
    )
}

context(patchContext: BytecodePatchContext)
private fun hookOtherChatInfoViewGeometry() {
    val otherChatInfoViewClass = OthersChatInfoViewClassFingerprint.classDef

    otherChatInfoViewClass.methods.first { it.name == "getTotalWidth" }
        .addAdditionalWidthHook()

    val makeRectMethod = otherChatInfoViewClass.methods.first { it.name == "makeRect" }
    val getBookmarkIconIndex = makeRectMethod.indexOfFirstInstructionOrThrow {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "getBookmarkIcon"
    }

    makeRectMethod.replaceInstruction(
        getBookmarkIconIndex,
        "invoke-virtual {p0}, $CHAT_INFO_VIEW_CLASS->getExtension()$CHAT_INFO_EXTENSION_CLASS",
    )
    makeRectMethod.addInstructionsWithLabels(
        getBookmarkIconIndex + 1,
        """
            move-result-object v1
            if-eqz v1, :cond_extension_rect
            invoke-virtual {p0}, Lcom/kakao/talk/widget/chatlog/OthersChatInfoView;->getTotalWidth()I
            move-result v3
            invoke-virtual {v1, v0, v3, v2}, $CHAT_INFO_EXTENSION_CLASS->calculateRect(III)I
            move-result v2
            :cond_extension_rect
            invoke-virtual {p0}, $CHAT_INFO_VIEW_CLASS->getBookmarkIcon()Landroid/graphics/Bitmap;
        """.trimIndent(),
    )
}

context(patchContext: BytecodePatchContext)
private fun hookMyChatInfoViewGeometry() {
    val myChatInfoViewClass = MyChatInfoViewClassFingerprint.classDef

    myChatInfoViewClass.methods.first { it.name == "getTotalWidth" }
        .addAdditionalWidthHook()

    val makeRectMethod = myChatInfoViewClass.methods.first { it.name == "makeRect" }
    val getDateLayoutIndex = makeRectMethod.indexOfFirstInstructionOrThrow {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "getDateLayout"
    }

    makeRectMethod.replaceInstruction(
        getDateLayoutIndex,
        "invoke-virtual {p0}, $CHAT_INFO_VIEW_CLASS->getExtension()$CHAT_INFO_EXTENSION_CLASS",
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
            invoke-virtual {v0, v3, v4, v2}, $CHAT_INFO_EXTENSION_CLASS->calculateRect(III)I
            move-result v2
            :cond_extension_rect
            invoke-virtual {p0}, $CHAT_INFO_VIEW_CLASS->getDateLayout()Landroid/text/Layout;
        """.trimIndent(),
    )
}

private fun MutableMethod.addAdditionalWidthHook() {
    val getPaddingLeftIndex = indexOfFirstInstructionOrThrow {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "getPaddingLeft"
    }

    addInstructionsWithLabels(
        getPaddingLeftIndex,
        """
            invoke-virtual {p0}, $CHAT_INFO_VIEW_CLASS->getExtension()$CHAT_INFO_EXTENSION_CLASS
            move-result-object v1
            if-eqz v1, :cond_extension_width
            invoke-virtual {v1}, $CHAT_INFO_EXTENSION_CLASS->getAdditionalWidth()I
            move-result v1
            invoke-static {v0, v1}, Ljava/lang/Math;->max(II)I
            move-result v0
            :cond_extension_width
            nop
        """.trimIndent(),
    )
}