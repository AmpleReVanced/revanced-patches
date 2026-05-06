package app.revanced.patches.dcinside.misc

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object QuickWriteVisibilityFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/post/fragments",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z"),
    returnType = "V",
    strings = listOf("postListQuickWrite"),
)

internal object PostListOnViewCreatedFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/post/fragments",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
    returnType = "V",
    strings = listOf("postListQuickWrite"),
)
