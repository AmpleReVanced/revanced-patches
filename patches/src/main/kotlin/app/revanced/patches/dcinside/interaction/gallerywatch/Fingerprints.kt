package app.revanced.patches.dcinside.interaction.gallerywatch

import app.morphe.patcher.Fingerprint
import app.revanced.patches.dcinside.layout.shortcut.PostListFragmentFingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.revanced.patches.dcinside.shared.recyclerViewHolderType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/GalleryWatchModePatch;"

internal val RECYCLER_VIEW_HOLDER get() = recyclerViewHolderType

internal object AutoRefreshTickFingerprint : Fingerprint(
    classFingerprint = PostListFragmentFingerprint,
    isStatic = true,
    parameters = listOf("L"),
    returnType = "L",
    filters = listOf(
        methodCall("Landroidx/recyclerview/widget/LinearLayoutManager;->findFirstVisibleItemPosition()I"),
    ),
)

internal object AutoRefreshDurationFingerprint : Fingerprint(
    classFingerprint = PostListFragmentFingerprint,
    parameters = emptyList(),
    returnType = "V",
    strings = listOf("list_auto_refresh_max_seconds"),
    filters = listOf(methodCall(parameters = listOf("Ljava/lang/String;"), returnType = "J")),
)

internal object PostListAdapterConstructorFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("L", "L", "Ljava/util/UUID;"),
    returnType = "V",
)

internal object PostListAdapterUpdateFingerprint : Fingerprint(
    classFingerprint = PostListAdapterConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "I", "L", "L", "L"),
    returnType = "V",
)

internal object PostListAdapterBindFingerprint : Fingerprint(
    classFingerprint = PostListAdapterConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(RECYCLER_VIEW_HOLDER, "I"),
    returnType = "V",
)

internal object PostListItemTypeFingerprint : Fingerprint(
    classFingerprint = PostListAdapterConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("I"),
    returnType = "I",
    filters = listOf(
        methodCall(
            parameters = listOf("I"),
            returnType = "Lcom/dcinside/app/response/PostItem;",
        ),
        methodCall(
            definingClass = "Lcom/dcinside/app/response/PostItem;",
            parameters = emptyList(),
            returnType = "I",
        ),
    ),
)

internal object GetPostsBridgeFingerprint : Fingerprint(
    definingClass = EXTENSION_CLASS,
    name = "getPosts",
)

internal object GetPostAtBridgeFingerprint : Fingerprint(
    definingClass = EXTENSION_CLASS,
    name = "getPostAt",
)

internal object GetPostNumberBridgeFingerprint : Fingerprint(
    definingClass = EXTENSION_CLASS,
    name = "getPostNumber",
)

internal object GetItemViewBridgeFingerprint : Fingerprint(
    definingClass = EXTENSION_CLASS,
    name = "getItemView",
)