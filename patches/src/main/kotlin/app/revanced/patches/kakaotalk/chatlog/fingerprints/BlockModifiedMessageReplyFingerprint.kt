package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object SwipeReplyMovementFlagsFingerprint : Fingerprint(
    name = "getMovementFlags",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Landroidx/recyclerview/widget/RecyclerView;", "L"),
    returnType = "I",
    custom = { _, classDef -> classDef.sourceFile == "ChatLogItemTouchHelperCallback.kt" },
)

/**
 * The single suspend composer both reply and comment sends route through; its first parameter is
 * the source chat log they quote (the reply/comment distinction is in the attachment args).
 */
internal object ReplyCommentSendFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(
        "Lcom/kakao/talk/db/model/chatlog/",
        "Ljava/lang/CharSequence;",
        "Lorg/json/JSONObject;",
        "Ljava/lang/String;",
        "Lkotlin/coroutines/Continuation;",
    ),
    returnType = "Ljava/lang/Object;",
    custom = { _, classDef -> classDef.sourceFile == "InputViewModel.kt" },
)