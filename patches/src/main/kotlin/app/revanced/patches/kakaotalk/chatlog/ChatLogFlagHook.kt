package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ChatLogVFieldPutBooleanFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.FlushToDBChatLogFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

context(patchContext: BytecodePatchContext)
internal fun addChatLogFlagAccessors(): ResolvedChatLogModel {
    val vFieldClass = ChatLogVFieldPutBooleanFingerprint.classDef
    val putBooleanMethod = ChatLogVFieldPutBooleanFingerprint.method

    ChatLogFlag.entries.forEach { flag ->
        vFieldClass.methods.add(createPutFlagMethod(vFieldClass.type, putBooleanMethod.name, flag))
        vFieldClass.methods.add(createGetFlagMethod(vFieldClass.type, flag))
    }

    val chatLogClass = ChatLogFingerprint.classDef
    val vFieldField = chatLogClass.fields.first { it.type == vFieldClass.type }

    return ResolvedChatLogModel(
        chatLogType = chatLogClass.type,
        vFieldType = vFieldClass.type,
        vFieldFieldName = vFieldField.name,
        flushToDBMethodName = FlushToDBChatLogFingerprint.method.name,
    )
}

private fun createPutFlagMethod(
    vFieldType: String,
    putBooleanMethodName: String,
    flag: ChatLogFlag,
) = ImmutableMethod(
    vFieldType,
    flag.putMethodName,
    listOf(ImmutableMethodParameter("Z", null, null)),
    "V",
    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
    null,
    null,
    MutableMethodImplementation(3),
).toMutable().apply {
    addInstructions(
        0,
        """
            const-string v0, "${flag.vFieldKey}"
            invoke-virtual {p0, v0, p1}, $vFieldType->$putBooleanMethodName(Ljava/lang/String;Z)V
            return-void
        """.trimIndent(),
    )
}

private fun createGetFlagMethod(
    vFieldType: String,
    flag: ChatLogFlag,
) = ImmutableMethod(
    vFieldType,
    flag.getMethodName,
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
            iget-object v0, p0, $vFieldType->a:Lorg/json/JSONObject;
            const-string v1, "${flag.vFieldKey}"
            const/4 v2, 0x0
            invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->optBoolean(Ljava/lang/String;Z)Z
            move-result v0
            return v0
        """.trimIndent(),
    )
}