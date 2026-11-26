package app.revanced.patches.kakaotalk.interaction.hiddenauthor

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object RewritesFeedTaskFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("J", "L", "Lkotlin/coroutines/Continuation;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "OpenChatRewritesFeedTask.kt" },
)

internal val RewritesFeedTaskFingerprint.feed
    get() = instructionMatches.last().fieldReference

internal object LocoChatLogFeedFingerprint : Fingerprint(
    parameters = listOf("L", "L"),
    filters = listOf(
        methodCall(parameters = emptyList(), returnType = "J"),
        methodCall(smali = "Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;"),
        methodCall(parameters = emptyList(), returnType = "J"),
        methodCall(parameters = emptyList(), returnType = "J"),
        methodCall(parameters = emptyList(), returnType = "Ljava/lang/String;"),
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "IOverwriteFeed.kt" &&
            method.parameterTypes.first().toString() == RewritesFeedTaskFingerprint.feed.type
    },
)

internal object MemberProfileNavigatorFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L"),
    strings = listOf("C002", "r"),
    custom = { _, classDef -> classDef.sourceFile == "ReactionUserListNavigator.kt" },
)

internal object FeedMessageFingerprint : Fingerprint(
    isStatic = false,
    returnType = "Ljava/lang/CharSequence;",
    parameters = listOf("L", "L", "J"),
    filters = listOf(
        methodCall(parameters = emptyList(), returnType = "Ljava/lang/String;"),
        methodCall(name = "generateMessage"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "FeedType.kt" },
)
