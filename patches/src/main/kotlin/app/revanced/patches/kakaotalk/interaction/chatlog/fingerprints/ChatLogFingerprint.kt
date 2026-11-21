package app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType.STARTS_WITH
import app.morphe.patcher.string
import app.morphe.util.findMethodFromToString
import com.android.tools.smali.dexlib2.AccessFlags

internal object ChatLogFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf(
        string("[class:"),
        string("] ChatLog [", STARTS_WITH),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLog.kt" },
)

internal fun chatLogOwnerIdGetter() =
    ChatLogFingerprint.originalMethod.findMethodFromToString("] ChatLog [ownerId=")