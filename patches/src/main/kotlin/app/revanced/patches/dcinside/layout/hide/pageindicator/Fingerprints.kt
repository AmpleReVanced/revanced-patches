package app.revanced.patches.dcinside.layout.hide.pageindicator

import app.morphe.patcher.Fingerprint
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags

private const val RECYCLER_VIEW_HOLDER =
    "Landroidx/recyclerview/widget/RecyclerView\$ViewHolder;"

internal object PostListPageIndicatorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    parameters = listOf("I", RECYCLER_VIEW_HOLDER, "I"),
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "post_section_text"),
    ),
)

internal object SearchResultPageIndicatorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(RECYCLER_VIEW_HOLDER, "I"),
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "post_section_text"),
    ),
)