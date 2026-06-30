package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ReactionMentionFlagPreferenceFingerprint : Fingerprint(
    strings = listOf("reaction_mention_flag"),
)

internal object ReactionMentionFlagFingerprint : Fingerprint(
    name = "invokeSuspend",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf(
        "call to 'resume' before 'invoke' with coroutine",
    ),
)
