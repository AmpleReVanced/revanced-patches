package app.revanced.patches.kakaotalk.chatroom.fingerprints

import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction

// 25.8.0 patch completed
internal val limitMentionToNonMultiChatFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    returns("V")
    strings(
        "fragment",
        "viewModelStoreOwner",
        "inputViewModel",
        "modalityPanelComponentProvider",
        "modalityPanelComponentViewModel",
        "chatRoom",
        "normalInputBoxController",
        "bottomViewController",
        "sharpSearchViewController",
        "keyboardWindowInsetsHelper",
        "setupStatusBarColor",
        "sendWarningStub",
        "getSendWarningView",
        "previewIndicatorStub",
        "getPreviewIndicatorView",
        "chatLogRecyclerList",
        "aiChatBotMentionCommandStub",
        "aiChatBotShortcutCommandStub",
        "aiChatBotDescriptionStub",
        "sharpSearchModeLayoutStub",
        "openLinkShoutModeLayoutStub",
        "getOpenLinkShoutModeLayoutView",
        "getSharpSearchModeLayoutView",
        "walkieTalkieLayoutViewStub",
        "inputWindowDisabledStub",
        "inputWindowKeyboardToolbarStub",
        "keywordEmoticonPreviewStub",
        "getChatLogSelectMode"
    )
    opcodes(
        Opcode.MOVE_OBJECT_FROM16, // × 17 times
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
    )
    custom { method, classDef -> classDef.sourceFile == "InputComponent.kt" }
}

// 25.8.0 patch completed
internal val mentionComponentClassFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    returns("V")
    strings("lifecycleOwner", "inputViewModel", "normalInputBoxController", "chatRoom")
    opcodes(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_BOOLEAN
    )
    custom { method, classDef -> classDef.sourceFile == "MentionComponent.kt" }
}

// 25.8.0 patch completed
internal val checkMentionableFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Landroid/widget/Filter${"$"}FilterResults;")
    parameters("Ljava/lang/CharSequence;")
    strings("@")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.INVOKE_INTERFACE,
    )
    custom { method, classDef -> classDef.sourceFile == "MentionItemListAdapter.kt" && method.name == "performFiltering" }
}

// 25.8.0 patch completed
internal val isMultiChatFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL)
    returns("Z")
    opcodes(
        Opcode.IF_NEZ,
        Opcode.CONST_4,
        Opcode.GOTO,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.AGET,
        Opcode.CONST_4,
        Opcode.IF_EQ,
        Opcode.CONST_4,
    )
    custom { method, classDef ->
        if (classDef.sourceFile != "ChatRoomTypeHelper.kt") return@custom false

        val instructions = method.instructions
        val constants = mutableSetOf<Int>()

        for (insn in instructions) {
            if (insn.opcode == Opcode.CONST_4) {
                val value = (insn as? NarrowLiteralInstruction)?.narrowLiteral
                if (value in listOf(5, 6, 7)) {
                    constants.add(value!!)
                }
            }
        }

        constants.containsAll(listOf(5, 6, 7))
    }
}