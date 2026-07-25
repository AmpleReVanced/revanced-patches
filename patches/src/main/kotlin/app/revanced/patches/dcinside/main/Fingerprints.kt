package app.revanced.patches.dcinside.main

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object MainAdapterItemsUpdateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    parameters = listOf("Ljava/util/List;"),
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Ljava/util/ArrayList;",
            name = "clear",
            parameters = emptyList(),
            returnType = "V",
        ),
        methodCall(
            definingClass = "Ljava/util/ArrayList;",
            name = "addAll",
            parameters = listOf("Ljava/util/Collection;"),
            returnType = "Z",
        ),
        methodCall(
            definingClass = "Landroidx/recyclerview/widget/RecyclerView\$Adapter;",
            name = "notifyDataSetChanged",
            parameters = emptyList(),
            returnType = "V",
        ),
    ),
)
