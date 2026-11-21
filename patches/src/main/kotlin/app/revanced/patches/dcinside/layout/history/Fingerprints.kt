package app.revanced.patches.dcinside.layout.history

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
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
        methodCall(
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
            returnType = "Landroid/text/SpannableStringBuilder;",
        ),
    ),
)

// Must be an instance method, as the comment is read from the second call site register.
internal object CommentAuthorLineFingerprint : Fingerprint(
    isStatic = true,
    returnType = "Landroid/text/SpannableStringBuilder;",
    parameters = listOf("L", "Ljava/lang/String;", "Ljava/lang/String;", "Landroid/content/Context;"),
    filters = listOf(string("owner")),
)

internal object CommentPatchIncludedFingerprint : Fingerprint(
    definingClass = EXTENSION_CLASS,
    name = "isCommentPatchIncluded",
)

internal object PostHistoryWriterFingerprint : Fingerprint(
    isStatic = true,
    parameters = listOf("L", POST_INFO_CLASS),
    returnType = "V",
    strings = listOf("postHistoryEnabled"),
)

internal object PostHistorySummaryBindFingerprint : Fingerprint(
    parameters = listOf("L", "I", "I"),
    returnType = "V",
    custom = { method, _ ->
        method.implementation?.instructions?.any {
            it.getReference<MethodReference>()?.definingClass == PostHistoryRealmProxyFingerprint.originalClassDef.superclass
        } == true
    },
)

internal const val POST_HISTORY_ACTIVITY_CLASS = "Lcom/dcinside/app/history/PostHistoryActivity;"

internal const val GALLERY_FILTER_EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/PostHistoryGalleryFilterPatch;"

internal const val GALLERY_FILTER_HOST_INTERFACE =
    "Lapp/revanced/extension/dcinside/patches/PostHistoryGalleryFilterPatch\$Host;"

internal object PostHistoryActivityOnCreateFingerprint : Fingerprint(
    definingClass = POST_HISTORY_ACTIVITY_CLASS,
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

// The only method that stops the list from laying out while the item manager is rebuilt.
internal object PostHistoryReloadFingerprint : Fingerprint(
    definingClass = POST_HISTORY_ACTIVITY_CLASS,
    isStatic = false,
    parameters = emptyList(),
    returnType = "V",
    filters = listOf(
        methodCall("Landroidx/recyclerview/widget/RecyclerView;->suppressLayout(Z)V"),
    ),
)

internal object PostHistoryQueryFingerprint : Fingerprint(
    parameters = listOf("L", "J", "J"),
    returnType = "Ljava/util/ArrayList;",
    strings = listOf("hide", "likes", "read", "reply"),
)

// The Realm proxy names every column next to the accessor that reads it,
// so the filters match the gallery accessors in declaration order.
internal object PostHistoryRealmProxyFingerprint : Fingerprint(
    name = "toString",
    parameters = emptyList(),
    returnType = "Ljava/lang/String;",
    strings = listOf("PostHistory = proxy[{key:"),
    filters = listOf(
        string("},{galleryId:"),
        methodCall(definingClass = "this", parameters = emptyList(), returnType = "Ljava/lang/String;"),
        string("},{galleryName:"),
        methodCall(definingClass = "this", parameters = emptyList(), returnType = "Ljava/lang/String;"),
    ),
)

internal object GalleryIdBridgeFingerprint : Fingerprint(
    definingClass = GALLERY_FILTER_EXTENSION_CLASS,
    name = "getGalleryId",
)

internal object GalleryNameBridgeFingerprint : Fingerprint(
    definingClass = GALLERY_FILTER_EXTENSION_CLASS,
    name = "getGalleryName",
)
