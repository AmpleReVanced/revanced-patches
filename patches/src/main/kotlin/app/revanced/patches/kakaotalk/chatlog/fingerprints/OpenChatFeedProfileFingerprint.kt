package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object ChatMessageFeedViewHolderBindFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = emptyList(),
    returnType = "V",
    strings = listOf("null cannot be cast to non-null type com.kakao.talk.db.model.chatlog.ChatLog"),
    filters = listOf(
        methodCall("Landroid/text/method/LinkMovementMethod;->getInstance()Landroid/text/method/MovementMethod;"),
        methodCall("Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatMessageFeedViewHolder.kt" },
)

internal object ChatMessageFeedViewHolderConfigureFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("targetChatId", "feedType", "members"),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatMessageFeedViewHolder.kt" && method.parameterTypes.size == 1
    },
)

internal object ChatLogViewHolderOpenLinkMemberIntentFingerprint : Fingerprint(
    returnType = "Landroid/content/Intent;",
    parameters = listOf(
        "Lcom/kakao/talk/db/model/Friend;",
        "Landroid/content/Context;",
        "Ljava/util/HashMap;",
    ),
    filters = listOf(
        methodCall(
            "Lcom/kakao/talk/module/openlink/contract/OpenLinkModuleFacade;" +
                    "->openLinkChatMemberIntent(Landroid/content/Context;Lcom/kakao/talk/db/model/Friend;ZJLcom/kakao/talk/openlink/db/model/OpenLink;Ljava/util/HashMap;)Landroid/content/Intent;"
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatLogViewHolder.kt" },
)

internal object FeedTypeLeaveGenerateMessageFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/lang/CharSequence;",
    strings = listOf("member", "kicked", "memorial"),
    custom = { method, classDef ->
        classDef.sourceFile == "FeedType.kt" && method.name == "generateMessage"
    },
)
