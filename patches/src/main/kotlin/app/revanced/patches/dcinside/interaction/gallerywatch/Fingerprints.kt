package app.revanced.patches.dcinside.interaction.gallerywatch

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/GalleryWatchModePatch;"

internal const val RECYCLER_VIEW_HOLDER =
    "Landroidx/recyclerview/widget/RecyclerView\$ViewHolder;"

private val autoRefreshDecisionCall = InstructionFilter { method, instruction ->
    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.let { reference ->
        reference.definingClass == method.definingClass &&
            reference.parameterTypes.singleOrNull()?.toString() == method.definingClass &&
            reference.returnType == "Z"
    } == true
}

internal object AutoRefreshTimeoutPromptFingerprint : Fingerprint(
    returnType = "L",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "auto_refresh_wait_max_out"),
    ),
)

internal object AutoRefreshTickFingerprint : Fingerprint(
    classFingerprint = AutoRefreshTimeoutPromptFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf("L", "Ljava/lang/Long;"),
    returnType = "L",
    filters = listOf(autoRefreshDecisionCall),
    custom = { method, classDef -> method.parameterTypes.firstOrNull().toString() == classDef.type },
)

internal object AutoRefreshDurationFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "J",
    strings = listOf("list_auto_refresh_max_seconds"),
)

internal object PostListAdapterConstructorFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("L", "L", "Ljava/util/UUID;"),
    returnType = "V",
    strings = listOf("fragment", "galleryMode", "listMappingId"),
)

internal object PostListAdapterUpdateFingerprint : Fingerprint(
    classFingerprint = PostListAdapterConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "I", "L", "L", "L"),
    returnType = "V",
    strings = listOf("response", "filter", "adHost"),
)

internal object PostListAdapterBindFingerprint : Fingerprint(
    classFingerprint = PostListAdapterConstructorFingerprint,
    name = "onBindViewHolder",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(RECYCLER_VIEW_HOLDER, "I"),
    returnType = "V",
)

internal object PostListItemTypeFingerprint : Fingerprint(
    classFingerprint = PostListAdapterConstructorFingerprint,
    name = "getItemViewType",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("I"),
    returnType = "I",
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