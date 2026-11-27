package app.revanced.patches.dcinside.layout.shortcut

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PostListFragmentFingerprint : Fingerprint(
    name = "onCreateView",
    parameters = listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;", "Landroid/os/Bundle;"),
    returnType = "Landroid/view/View;",
    filters = listOf(resourceLiteral(ResourceType.LAYOUT, "fragment_post_list")),
)

internal object QuickWriteVisibilityFingerprint : Fingerprint(
    classFingerprint = PostListFragmentFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z"),
    returnType = "V",
    filters = listOf(
        methodCall("Landroid/view/View;->setVisibility(I)V"),
        methodCall("Landroid/view/View;->getVisibility()I"),
    ),
)

internal object PostListOnViewCreatedFingerprint : Fingerprint(
    classFingerprint = PostListFragmentFingerprint,
    name = "onViewCreated",
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
    returnType = "V",
    filters = listOf(methodCall(name = "onViewCreated", opcode = Opcode.INVOKE_SUPER)),
)
