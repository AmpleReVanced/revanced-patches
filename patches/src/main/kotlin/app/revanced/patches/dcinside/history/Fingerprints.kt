package app.revanced.patches.dcinside.history

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

internal const val POST_INFO_CLASS = "Lcom/dcinside/app/model/PostInfo;"
internal const val POST_ITEM_CLASS = "Lcom/dcinside/app/response/PostItem;"

internal const val EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/AuthorIdentifierPatch;"

internal object PostAuthorLineFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/view/PostReadHeaderView;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(POST_INFO_CLASS, "Z", "Ljava/lang/String;"),
    returnType = "V",
    filters = listOf(
        methodCall(returnType = "Landroid/text/Spannable;"),
    ),
)

// Must be an instance method, as the comment is read from the second call site register.
internal object CommentAuthorLineFingerprint : Fingerprint(
    returnType = "Landroid/text/SpannableStringBuilder;",
    parameters = listOf("L", "Ljava/lang/String;", "Ljava/lang/String;", "Landroid/content/Context;"),
    filters = listOf(string("owner")),
    custom = { method, _ -> !AccessFlags.STATIC.isSet(method.accessFlags) },
)

internal object CommentUserIdBridgeFingerprint : Fingerprint(
    definingClass = EXTENSION_CLASS,
    name = "getCommentUserId",
)

internal object CommentPatchIncludedFingerprint : Fingerprint(
    definingClass = EXTENSION_CLASS,
    name = "isCommentPatchIncluded",
)

internal object PostHistoryWriterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    parameters = listOf("L", POST_INFO_CLASS),
    returnType = "V",
    strings = listOf("this.createObject(T::class.java, primaryKeyValue)"),
)

internal object PostHistorySummaryBindFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/history/",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Lcom/codewaves/stickyheadergrid/a\$b;", "I", "I"),
    returnType = "V",
    filters = listOf(
        methodCall(returnType = "Landroid/text/Spannable;"),
    ),
)
