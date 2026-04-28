package app.revanced.patches.kakaotalk.chatlog

import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val CHAT_INFO_EXTENSION_CLASS = "Lapp/revanced/extension/kakaotalk/chatlog/ChatInfoExtension;"
internal const val CHAT_INFO_VIEW_CLASS = "Lcom/kakao/talk/widget/chatlog/ChatInfoView;"
internal const val CONTINUATION_TYPE = "Lkotlin/coroutines/Continuation;"
internal const val FUNCTION0_TYPE = "Lkotlin/jvm/functions/Function0;"
internal const val KOTLIN_UNIT_TYPE = "Lkotlin/Unit;"
internal const val OBJECT_TYPE = "Ljava/lang/Object;"

internal enum class ChatLogFlag(
    val vFieldKey: String,
    val putMethodName: String,
    val getMethodName: String,
    val extensionSetterName: String,
) {
    Deleted("_revanced_deleted", "putDeleted", "getDeleted", "setDeleted"),
    Hidden("_revanced_hidden", "putHidden", "getHidden", "setHidden"),
}

internal data class ResolvedChatLogModel(
    val chatLogType: String,
    val vFieldType: String,
    val vFieldFieldName: String,
    val flushToDBMethodName: String,
) {
    fun setFlagAndFlushInstructions(managerClass: String, flag: ChatLogFlag) = """
        iget-object v0, p1, $chatLogType->$vFieldFieldName:$vFieldType
        const/4 v1, 0x1
        invoke-virtual {v0, v1}, $vFieldType->${flag.putMethodName}(Z)V
        invoke-virtual {p0, p1}, $managerClass->$flushToDBMethodName($chatLogType)Z
        return-void
    """.trimIndent()
}

internal fun MethodReference.matchesNoArgReturn(returnType: String) =
    parameterTypes.isEmpty() && this.returnType == returnType

internal fun MethodReference.matchesSingleArgReturn(parameterType: String, returnType: String) =
    parameterTypes == listOf(parameterType) && this.returnType == returnType

internal fun MethodReference.matchesChatLogDaoWrite(chatLogType: String) =
    returnType == "V" && parameterTypes == listOf(chatLogType, FUNCTION0_TYPE)

internal fun MethodReference.matchesSyncChatLogDaoWrite(chatLogDaoClass: String, chatLogType: String) =
    definingClass == chatLogDaoClass &&
            returnType == OBJECT_TYPE &&
            parameterTypes == listOf(chatLogType, CONTINUATION_TYPE)