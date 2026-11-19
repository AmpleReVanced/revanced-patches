package app.revanced.patches.dcinside.shared

import app.morphe.patcher.Fingerprint

private object RecyclerViewHolderFingerprint : Fingerprint(
    definingClass = "Landroidx/recyclerview/widget/RecyclerView;",
    name = "getChildViewHolder",
    parameters = listOf("Landroid/view/View;"),
    returnType = "L",
)

internal val recyclerViewHolderType get() = RecyclerViewHolderFingerprint.originalMethod.returnType
