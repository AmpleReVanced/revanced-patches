package app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal fun CanReactToChatLogFingerprint(chatLogType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(chatLogType, "L"),
    returnType = "Z",
    custom = { _, classDef -> classDef.sourceFile == "ReactionManager.kt" },
)
