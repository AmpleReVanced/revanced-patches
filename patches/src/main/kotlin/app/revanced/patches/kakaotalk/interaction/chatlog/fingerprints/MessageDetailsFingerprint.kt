package app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ChatContextMenuActionLabelFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = emptyList(),
    returnType = "Ljava/lang/String;",
    strings = listOf("Undefined"),
    filters = listOf(methodCall("Landroid/content/Context;->getString(I)Ljava/lang/String;")),
    custom = { _, classDef -> classDef.sourceFile == "ChatContextMenuAction.kt" },
)

internal fun realMessageActionsFingerprint(chatLogType: String) = object : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(chatLogType, "L", "Ljava/lang/Object;"),
    returnType = "Ljava/util/Set;",
    strings = listOf("Required value was null."),
    filters = listOf(methodCall("Ljava/util/Map;->values()Ljava/util/Collection;")),
    custom = { _, classDef -> classDef.sourceFile == "RealAction.kt" },
) {}

internal fun threadMessageActionFilterFingerprint(actionType: String) = object : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(actionType),
    returnType = "Z",
    filters = listOf(opcode(Opcode.INSTANCE_OF), opcode(Opcode.IF_NEZ)),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogContextMenuManager.kt" },
) {}